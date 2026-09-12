package com.pilotothegreat.deencompanion.di

import com.pilotothegreat.deencompanion.alarms.PrayerAlarmScheduler
import com.pilotothegreat.deencompanion.data.athkar.AthkarRepository
import com.pilotothegreat.deencompanion.data.db.AppDatabase
import com.pilotothegreat.deencompanion.data.hadith.HadithRepository
import com.pilotothegreat.deencompanion.data.location.CityIndex
import com.pilotothegreat.deencompanion.data.location.LocationRepository
import com.pilotothegreat.deencompanion.data.backup.BackupRepository
import com.pilotothegreat.deencompanion.data.moment.MomentRepository
import com.pilotothegreat.deencompanion.data.nature.EarthquakeRepository
import com.pilotothegreat.deencompanion.data.prayer.PrayerLogRepository
import com.pilotothegreat.deencompanion.data.nature.EclipseRepository
import com.pilotothegreat.deencompanion.data.weather.WeatherRepository
import com.pilotothegreat.deencompanion.data.quran.KhatmaRepository
import com.pilotothegreat.deencompanion.data.quran.QuranRepository
import com.pilotothegreat.deencompanion.data.settings.SettingsRepository
import com.pilotothegreat.deencompanion.data.settings.createAppDataStore
import com.pilotothegreat.deencompanion.data.tasbih.TasbihRepository
import com.pilotothegreat.deencompanion.data.update.ApkInstaller
import com.pilotothegreat.deencompanion.data.update.UpdateChecker
import com.pilotothegreat.deencompanion.playback.QuranPlayer
import com.pilotothegreat.deencompanion.ui.athkar.AthkarSessionViewModel
import com.pilotothegreat.deencompanion.ui.athkar.AthkarViewModel
import com.pilotothegreat.deencompanion.ui.hadith.HadithBookViewModel
import com.pilotothegreat.deencompanion.ui.hadith.HadithViewModel
import com.pilotothegreat.deencompanion.ui.home.HomeViewModel
import com.pilotothegreat.deencompanion.ui.location.LocationViewModel
import com.pilotothegreat.deencompanion.ui.qibla.QiblaViewModel
import com.pilotothegreat.deencompanion.ui.quran.QuranViewModel
import com.pilotothegreat.deencompanion.ui.reader.ReaderViewModel
import com.pilotothegreat.deencompanion.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { createAppDataStore(androidContext()) }
    single { SettingsRepository(get()) }
    single { TasbihRepository(get()) }
    single { AthkarRepository({ androidContext().assets.open("athkar.json").bufferedReader().use { it.readText() } }, get()) }
    single { AppDatabase.build(androidContext()) }
    single { get<AppDatabase>().bookmarkDao() }
    single { get<AppDatabase>().hadithDao() }
    single { get<AppDatabase>().readingPlanDao() }
    single { KhatmaRepository(get()) }
    single { QuranRepository(androidContext(), get()) }
    single { HadithRepository(androidContext(), get()) }
    single { CityIndex { androidContext().assets.open("cities.json").bufferedReader().use { it.readText() } } }
    single { LocationRepository(androidContext(), get(), get()) }
    single { WeatherRepository() }
    single { get<AppDatabase>().naturalEventDao() }
    single { get<AppDatabase>().prayerLogDao() }
    single { PrayerLogRepository(get()) }
    single { BackupRepository(get(), get(), get()) }
    single { EclipseRepository(androidContext()) }
    single { EarthquakeRepository(get()) }
    single { MomentRepository(get(), get(), get(), get(), get()) }
    single { UpdateChecker(androidContext(), get()) }
    single { ApkInstaller(androidContext()) }
    single { PrayerAlarmScheduler(androidContext(), get()) }
    single { QuranPlayer(androidContext(), get(), get()) }

    viewModelOf(::HomeViewModel)
    viewModelOf(::QuranViewModel)
    viewModel { params -> ReaderViewModel(params.get(), get(), get(), get(), get()) }
    viewModelOf(::HadithViewModel)
    viewModel { params -> HadithBookViewModel(params.get(), get()) }
    viewModelOf(::QiblaViewModel)
    viewModel { SettingsViewModel(androidContext(), get(), get(), get(), get(), get(), get(), get()) }
    viewModelOf(::LocationViewModel)
    viewModelOf(::AthkarViewModel)
    viewModel { params -> AthkarSessionViewModel(params.get(), get(), get()) }
}
