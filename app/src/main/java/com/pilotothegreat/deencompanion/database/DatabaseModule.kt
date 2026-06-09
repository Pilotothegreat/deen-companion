// FIXED: Add try-catch around singleton initializations in DatabaseModule to log instantiation failures
package com.pilotothegreat.deencompanion.database

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import timber.log.Timber

val databaseModule = module {
    single {
        try {
            AppPreferenceRepo(get(), get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create AppPreferenceRepo")
            throw e
        }
    }

    single {
        try {
            val isRobolectric = try {
                Class.forName("org.robolectric.Robolectric") != null
            } catch (e: Exception) {
                false
            }
            if (isRobolectric) {
                Room.inMemoryDatabaseBuilder(
                    androidContext(),
                    AppDatabase::class.java
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            } else {
                Room.databaseBuilder(
                    androidContext(),
                    AppDatabase::class.java,
                    "database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
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

    single {
        try {
            get<AppDatabase>().hadithDao()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get hadithDao")
            throw e
        }
    }

    single {
        try {
            get<AppDatabase>().tasbihDao()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get tasbihDao")
            throw e
        }
    }

    single {
        try {
            HadithRepository(androidContext(), get())
        } catch (e: Exception) {
            Timber.e(e, "Failed to create HadithRepository")
            throw e
        }
    }
}

