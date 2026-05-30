// FIXED: Add try-catch around singleton initializations in DatabaseModule to log instantiation failures
package com.pilotothegreat.deencompanion.database

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import timber.log.Timber

val databaseModule = module {
    single {
        try {
            AppPreferenceRepo(get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create AppPreferenceRepo")
            throw e
        }
    }

    single {
        try {
            Room.databaseBuilder(
                androidContext(),
                AppDatabase::class.java,
                "database"
            )
                .fallbackToDestructiveMigration()
                .build()
        } catch (e: Exception) {
            Timber.e(e, "Failed to build AppDatabase")
            throw e
        }
    }

    single {
        try {
            get<AppDatabase>().bookmarkedVerseDao()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get bookmarkedVerseDao")
            throw e
        }
    }
}

