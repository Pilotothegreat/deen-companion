package com.pilotothegreat.deencompanion.ui.navigation

import org.koin.dsl.module
import timber.log.Timber

val navigationModule = module {
    single {
        try {
            Navigator(startDestination = OverviewKey)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create Navigator")
            throw e
        }
    }
}
