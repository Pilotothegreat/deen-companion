// FIXED: Add UncaughtExceptionHandler to log crash details before application exit
package com.leekleak.trafficlight

import android.app.Application
import com.leekleak.trafficlight.database.databaseModule
import com.leekleak.trafficlight.model.managerModule
import com.leekleak.trafficlight.ui.navigation.navigationModule
import com.leekleak.trafficlight.ui.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber
import com.leekleak.trafficlight.services.IqamaAlarmManager

class DeenApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // ADD crash logging
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Timber.e(exception, "UNCAUGHT EXCEPTION on thread: $thread")
            android.util.Log.e("CRASH", "Thread: ${thread.name}", exception)
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
}
