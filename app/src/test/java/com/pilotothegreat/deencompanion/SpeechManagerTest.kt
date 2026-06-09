package com.pilotothegreat.deencompanion

import android.content.Context
import com.pilotothegreat.deencompanion.services.AudioInputProvider
import com.pilotothegreat.deencompanion.services.SpeechManager
import com.pilotothegreat.deencompanion.services.SpeechState
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechManagerTest {

    @Test
    fun testSilenceThresholdTriggersStop() = runTest {
        val context = mockk<Context>(relaxed = true)

        // Mock AudioInputProvider that feeds silent data (0 amplitude)
        val mockProvider = object : AudioInputProvider {
            var recording = false
            var readCount = 0

            override fun startRecording(): Boolean {
                recording = true
                return true
            }

            override fun stopRecording() {
                recording = false
            }

            override fun read(buffer: ShortArray, size: Int): Int {
                // Feed silence (0s)
                for (i in 0 until size) {
                    buffer[i] = 0
                }
                readCount++
                if (readCount > 25) {
                    recording = false
                }
                return size
            }

            override fun isRecording(): Boolean = recording
        }

        val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        var virtualTime = 0L
        val speechManager = SpeechManager(
            context = context,
            audioInputProvider = mockProvider,
            assistantProxyProvider = { Pair("سورة البقرة", "Al-Baqarah translation") },
            dispatcher = testDispatcher,
            timeProvider = {
                val t = virtualTime
                virtualTime += 100
                t
            }
        )

        val states = mutableListOf<SpeechState>()
        val job = launch {
            speechManager.state.toList(states)
        }

        // Trigger listening
        speechManager.startListening()

        // Wait for VAD silence accumulator (needs 2000ms / 20 check intervals of 100ms)
        kotlinx.coroutines.delay(2500)
        speechManager.stopListening()

        // Verify that the VAD auto-stop and transition to Processing was triggered
        val hasTriggeredProcessing = states.any { 
            it is SpeechState.Processing || it is SpeechState.ConfidenceCheck || it is SpeechState.PlayingTajweed 
        }
        assertTrue("VAD should automatically trigger processing on 2000ms of silence", hasTriggeredProcessing)

        job.cancel()
        speechManager.release()
    }
}
