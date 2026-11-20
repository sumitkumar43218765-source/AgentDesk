package com.agentdesk.app.app

import android.app.Application
import com.agentdesk.app.core.logging.AppLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AgentApplication : Application() {

    @Inject
    lateinit var logger: AppLogger

    override fun onCreate() {
        super.onCreate()
        logger.i("AgentApplication", "AgentDesk initialized successfully")
    }
}
