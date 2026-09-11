package com.pilotothegreat.deencompanion.data.net

import com.pilotothegreat.deencompanion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("User-Agent", userAgent)
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code for $url")

            val total = connection.contentLengthLong
            val output = ByteArrayOutputStream(if (total in 1..Int.MAX_VALUE) total.toInt() else 64 * 1024)
            connection.inputStream.use { input ->
                val buffer = ByteArray(64 * 1024)
                var received = 0L
                while (true) {
                    ensureActive()
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    received += read
                    if (total > 0) onProgress((received.toFloat() / total).coerceAtMost(1f))
                }
            }
            output.toString(Charsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }
}
