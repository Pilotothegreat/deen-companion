package com.leekleak.trafficlight.ui

import com.leekleak.trafficlight.ui.overview.OverviewVM
import com.leekleak.trafficlight.ui.settings.SettingsVM
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
}