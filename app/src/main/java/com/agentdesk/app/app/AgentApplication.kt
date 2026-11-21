package com.agentdesk.app.app

import android.app.Application
import android.os.StrictMode
import com.agentdesk.app.BuildConfig
import com.agentdesk.app.core.logging.AppLogger
import com.agentdesk.app.core.notification.AppNotifier
import com.agentdesk.app.feature.settings.data.CrashReporter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AgentApplication : Application() {

    @Inject
    lateinit var logger: AppLogger

    @Inject
    lateinit var appNotifier: AppNotifier

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()
        crashReporter.install()
        appNotifier.createChannels()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        logger.i("AgentApplication", "AgentDesk initialized successfully")
    }
}
