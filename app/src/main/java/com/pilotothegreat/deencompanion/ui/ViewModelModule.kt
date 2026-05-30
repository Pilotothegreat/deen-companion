package com.pilotothegreat.deencompanion.ui

import com.pilotothegreat.deencompanion.ui.overview.OverviewVM
import com.pilotothegreat.deencompanion.ui.settings.SettingsVM
import com.pilotothegreat.deencompanion.ui.hadith.HadithVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

val viewModelModule = module {
    viewModel {
        try {
            OverviewVM(get(), get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create OverviewVM")
            throw e
        }
    }
    viewModel {
        try {
            SettingsVM(get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create SettingsVM")
            throw e
        }
    }
    viewModel {
        try {
            HadithVM(get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create HadithVM")
            throw e
        }
    }
}
