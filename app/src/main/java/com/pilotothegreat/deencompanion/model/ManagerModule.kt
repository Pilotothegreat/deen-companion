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
}
