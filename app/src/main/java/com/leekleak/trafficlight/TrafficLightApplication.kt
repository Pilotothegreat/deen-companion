package com.leekleak.trafficlight

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import com.leekleak.trafficlight.database.databaseModule
import com.leekleak.trafficlight.model.managerModule
import com.leekleak.trafficlight.ui.navigation.navigationModule
import com.leekleak.trafficlight.ui.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber

class TrafficLightApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()
        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@TrafficLightApplication)
            modules(
                databaseModule,
                managerModule,
                viewModelModule,
                navigationModule
            )
        }
    }

    private fun createNotificationChannels() {
        val iqamaChannel = NotificationChannel(
            "iqama_notifications",
            "Iqama Notifications",
            IMPORTANCE_HIGH
        ).apply {
            description = "Notifies when it is time for Iqama prayer"
            enableVibration(true)
            setShowBadge(true)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(iqamaChannel)
    }
}