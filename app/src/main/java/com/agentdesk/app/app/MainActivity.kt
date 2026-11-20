package com.agentdesk.app.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.agentdesk.app.app.navigation.AppNavHost
import com.agentdesk.app.app.navigation.NavGraphRegistrar
import com.agentdesk.app.core.datastore.AppPreferences
import com.agentdesk.app.core.ui.theme.AgentDeskTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var registrars: Set<@JvmSuppressWildcards NavGraphRegistrar>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by appPreferences.themeMode.collectAsState(initial = "system")
            val dynamicColor by appPreferences.dynamicColor.collectAsState(initial = true)

            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            AgentDeskTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor
            ) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    registrars = registrars
                )
            }
        }
    }
}
