package com.pilotothegreat.deencompanion.playback

import androidx.media3.common.PlaybackException
import com.pilotothegreat.deencompanion.data.net.NetError
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * What the reader is told when an ayah will not play.
 *
 * "Check your connection" is the wrong answer to a file that is not on the server, and the right
 * answer to one that is. The mapping is the only thing standing between those two.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class PlaybackErrorsTest {

    private fun error(code: Int) = PlaybackException("", null, code).toNetError()

    @Test fun aDeadConnectionSaysSo() {
        assertEquals(NetError.OFFLINE, error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED))
        assertEquals(NetError.OFFLINE, error(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT))
    }

    @Test fun aMissingAyahIsNotAConnectionProblem() {
        assertEquals(NetError.NOT_FOUND, error(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))
    }

    @Test fun aServerSaidNoIsAFailure() {
        assertEquals(NetError.FAILED, error(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS))
    }

    @Test fun anythingElseFallsBackRatherThanGuessing() {
        assertEquals(NetError.FAILED, error(PlaybackException.ERROR_CODE_DECODING_FAILED))
        assertEquals(NetError.FAILED, error(PlaybackException.ERROR_CODE_UNSPECIFIED))
    }
}
