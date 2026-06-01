// FIXED: Add try-catch block in ManagerModule to identify creation failures
package com.pilotothegreat.deencompanion.model

import coil3.ImageLoader
import coil3.request.crossfade
import com.pilotothegreat.deencompanion.services.QuranPlaybackManager
import org.koin.dsl.module
import timber.log.Timber

val managerModule = module {
    single {
        try {
            ImageLoader.Builder(get())
                .crossfade(true)
                .build()
        } catch (e: Exception) {
            Timber.e(e, "Failed to create ImageLoader")
            throw e
        }
    }

    single {
        try {
            QuranPlaybackManager(get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create QuranPlaybackManager")
            throw e
        }
    }

    single<com.pilotothegreat.deencompanion.services.AudioInputProvider> {
        try {
            com.pilotothegreat.deencompanion.services.DefaultAudioInputProvider(get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create DefaultAudioInputProvider")
            throw e
        }
    }

    single {
        try {
            com.pilotothegreat.deencompanion.services.AssistantProxyService(get(), get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create AssistantProxyService")
            throw e
        }
    }

    single {
        try {
            val proxy: com.pilotothegreat.deencompanion.services.AssistantProxyService = get()
            com.pilotothegreat.deencompanion.services.SpeechManager(
                context = get(),
                audioInputProvider = get(),
                assistantProxyProvider = { query -> proxy.query(query) }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create SpeechManager")
            throw e
        }
    }
}
