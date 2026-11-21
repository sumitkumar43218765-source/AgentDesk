package com.agentdesk.app.feature.settings.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agentdesk.app.feature.settings.domain.PermissionChecker
import com.agentdesk.app.feature.settings.domain.model.PermissionItem
import com.agentdesk.app.feature.settings.domain.model.PermissionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionChecker {

    override fun getPermissions(): List<PermissionItem> {
        val items = mutableListOf<PermissionItem>()

        // 1. Notifications
        val notifState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                PermissionState.GRANTED
            } else {
                PermissionState.NOT_GRANTED
            }
        } else {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PermissionState.GRANTED
            } else {
                PermissionState.NOT_GRANTED
            }
        }
        val notifIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        items.add(
            PermissionItem(
                id = "notifications",
                title = "Notifications",
                description = "Required to show agent status, approvals, and build outcomes",
                state = notifState,
                actionIntent = notifIntent
            )
        )

        // 2. Install unknown packages
        val installState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context.packageManager.canRequestPackageInstalls()) {
                PermissionState.GRANTED
            } else {
                PermissionState.NOT_GRANTED
            }
        } else {
            PermissionState.GRANTED
        }
        val installIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else null
        items.add(
            PermissionItem(
                id = "install_packages",
                title = "Install Unknown Apps",
                description = "Required to install built APKs directly on your phone",
                state = installState,
                actionIntent = installIntent
            )
        )

        // 3. Battery optimization exemption
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                PermissionState.GRANTED
            } else {
                PermissionState.NOT_GRANTED
            }
        } else {
            PermissionState.NOT_NEEDED
        }
        val batteryIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else null
        items.add(
            PermissionItem(
                id = "battery_optimization",
                title = "Battery Optimization Exemption",
                description = "Prevents Android from killing long background builds and agent tasks",
                state = batteryState,
                actionIntent = batteryIntent
            )
        )

        return items
    }

    override fun observePermissions(): Flow<List<PermissionItem>> = flow {
        emit(getPermissions())
    }
}
