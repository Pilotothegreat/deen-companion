// FIXED: Add UncaughtExceptionHandler to log crash details before application exit
package com.pilotothegreat.deencompanion

import android.app.Application
import com.pilotothegreat.deencompanion.database.databaseModule
import com.pilotothegreat.deencompanion.model.managerModule
import com.pilotothegreat.deencompanion.ui.navigation.navigationModule
import com.pilotothegreat.deencompanion.ui.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber
import com.pilotothegreat.deencompanion.services.IqamaAlarmManager
import com.pilotothegreat.deencompanion.services.QuranPlaybackManager

class DeenApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ADD crash logging
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Timber.e(exception, "UNCAUGHT EXCEPTION on thread: $thread")
            android.util.Log.e("CRASH", "Thread: ${thread.name}", exception)
            defaultHandler?.uncaughtException(thread, exception)
        }

        IqamaAlarmManager.createNotificationChannel(this)
        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@DeenApplication)
            modules(
                databaseModule,
                managerModule,
                viewModelModule,
                navigationModule
            )
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            val playbackManager: QuranPlaybackManager = org.koin.core.context.GlobalContext.get().get()
            playbackManager.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing QuranPlaybackManager in onTerminate")
        }
    }
}
