package com.pilotothegreat.deencompanion.data.net

import com.pilotothegreat.deencompanion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object Http {
    private val userAgent = "Bilal/${BuildConfig.VERSION_NAME} (Android)"

    /**
     * Downloads [url] as UTF-8 text. [onProgress] receives 0..1 when the server reports a size.
     * Cancelling the calling coroutine stops the transfer.
     */
    suspend fun getText(
        url: String,
        timeoutMs: Int = 15_000,
        headers: Map<String, String> = emptyMap(),
        onProgress: (Float) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {
        open(url, timeoutMs, headers) { connection ->
            val total = connection.contentLengthLong
            val output = ByteArrayOutputStream(if (total in 1..Int.MAX_VALUE) total.toInt() else 64 * 1024)
            copy(connection, total, onProgress) { buffer, read -> output.write(buffer, 0, read) }
            output.toString(Charsets.UTF_8.name())
        }
    }

    /** The response parsed as a JSON object. */
    suspend fun getJson(url: String, timeoutMs: Int = 15_000, headers: Map<String, String> = emptyMap()): JSONObject =
        JSONObject(getText(url, timeoutMs, headers))

    /**
     * Downloads [url] into [destination], writing through a temporary file so a cancelled or failed
     * transfer never leaves a half-written file in place. When [expectedSha256] is given the file is
     * kept only if it matches, which is what makes downloaded audio safe to play.
     */
    suspend fun download(
        url: String,
        destination: File,
        expectedSha256: String? = null,
        timeoutMs: Int = 30_000,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs()
        val partial = File(destination.parentFile, "${destination.name}.part")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            open(url, timeoutMs, emptyMap()) { connection ->
                val total = connection.contentLengthLong
                partial.outputStream().use { out ->
                    copy(connection, total, onProgress) { buffer, read ->
                        out.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (expectedSha256 != null && !actual.equals(expectedSha256, ignoreCase = true)) {
                throw IOException("Checksum mismatch for $url")
            }
            if (destination.exists()) destination.delete()
            if (!partial.renameTo(destination)) throw IOException("Couldn't move the download into place")
            destination
        } finally {
            partial.delete()
        }
    }

    private suspend inline fun <T> open(url: String, timeoutMs: Int, headers: Map<String, String>, body: (HttpURLConnection) -> T): T {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("User-Agent", userAgent)
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw HttpException(code, url)
            return body(connection)
        } finally {
            connection.disconnect()
        }
    }

    private suspend inline fun copy(
        connection: HttpURLConnection,
        total: Long,
        onProgress: (Float) -> Unit,
        write: (ByteArray, Int) -> Unit,
    ) {
        connection.inputStream.use { input ->
            val buffer = ByteArray(64 * 1024)
            var received = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read == -1) break
                write(buffer, read)
                received += read
                if (total > 0) onProgress((received.toFloat() / total).coerceAtMost(1f))
            }
        }
    }
}

/** Carries the status code so callers can tell "rate limited" from "not found". */
class HttpException(val code: Int, url: String) : IOException("HTTP $code for $url")
