package com.agentdesk.app.feature.settings.presentation

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.settings.domain.model.SettingsSection

@Composable
fun SettingsRoute(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    SettingsScreen(
        onNavigateToSection = { section ->
            when (section) {
                SettingsSection.PROVIDERS -> navController.navigate(Routes.ProviderList)
                SettingsSection.AGENT -> navController.navigate(Routes.SettingsAgent)
                SettingsSection.ENVIRONMENT -> navController.navigate(Routes.SettingsEnvironment)
                SettingsSection.PROJECTS -> navController.navigate(Routes.SettingsProjects)
                SettingsSection.TERMINAL -> navController.navigate(Routes.SettingsTerminal)
                SettingsSection.APPEARANCE -> navController.navigate(Routes.SettingsAppearance)
                SettingsSection.NOTIFICATIONS -> {
                    runCatching {
                        val intent = Intent().apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            } else {
                                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                putExtra("app_package", context.packageName)
                                putExtra("app_uid", context.applicationInfo.uid)
                            }
                        }
                        context.startActivity(intent)
                    }
                }
                SettingsSection.PERMISSIONS -> navController.navigate(Routes.SettingsPermissions)
                SettingsSection.DATA_PRIVACY -> navController.navigate(Routes.SettingsDataPrivacy)
                SettingsSection.ABOUT -> navController.navigate(Routes.SettingsAbout)
            }
        },
        onBackClick = { navController.popBackStack() },
        modifier = modifier
    )
}
