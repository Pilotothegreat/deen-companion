package com.pilotothegreat.deencompanion.data

import com.pilotothegreat.deencompanion.data.net.HttpException
import com.pilotothegreat.deencompanion.data.net.NetError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** A rate-limited update check and a plane-mode failure used to look identical to the user. */
class NetErrorTest {

    @Test fun noConnectionReadsAsOffline() {
        assertEquals(NetError.OFFLINE, NetError.of(UnknownHostException("api.github.com")))
        assertEquals(NetError.OFFLINE, NetError.of(SocketTimeoutException()))
        assertEquals(NetError.OFFLINE, NetError.of(IOException("wrapped", UnknownHostException())))
    }

    @Test fun githubThrottlingIsItsOwnCase() {
        assertEquals(NetError.RATE_LIMITED, NetError.of(HttpException(403, "https://api.github.com/x")))
        assertEquals(NetError.RATE_LIMITED, NetError.of(HttpException(429, "https://api.github.com/x")))
    }

    @Test fun missingThingsAndEverythingElse() {
        assertEquals(NetError.NOT_FOUND, NetError.of(HttpException(404, "https://everyayah.com/1.mp3")))
        assertEquals(NetError.FAILED, NetError.of(HttpException(500, "https://example.com")))
        assertEquals(NetError.FAILED, NetError.of(IllegalStateException("bad json")))
    }
}
