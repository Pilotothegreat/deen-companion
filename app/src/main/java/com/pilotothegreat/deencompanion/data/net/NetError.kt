package com.pilotothegreat.deencompanion.data.net

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Why a request failed, so screens can say something true. Before this, a rate-limited update check
 * and a plane-mode failure produced the same message.
 */
enum class NetError {
    /** No usable connection, or the host never answered. */
    OFFLINE,

    /** The service asked us to slow down (GitHub allows 60 anonymous requests an hour). */
    RATE_LIMITED,

    /** The thing we asked for isn't there. */
    NOT_FOUND,

    /** Anything else, including a malformed response. */
    FAILED;

    companion object {
        fun of(error: Throwable): NetError = when {
            error is UnknownHostException || error is SocketTimeoutException -> OFFLINE
            error is HttpException && (error.code == 403 || error.code == 429) -> RATE_LIMITED
            error is HttpException && error.code == 404 -> NOT_FOUND
            error is IOException && error.cause is UnknownHostException -> OFFLINE
            else -> FAILED
        }
    }
}
