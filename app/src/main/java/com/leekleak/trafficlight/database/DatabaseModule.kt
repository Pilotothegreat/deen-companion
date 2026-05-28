package com.leekleak.trafficlight.database

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { AppPreferenceRepo(get()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().bookmarkedVerseDao() }
}
