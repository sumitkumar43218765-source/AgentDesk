package com.agentdesk.app.feature.build.data.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Singleton

@Singleton
class InstallResultReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INSTALL_STATUS = "com.agentdesk.app.ACTION_INSTALL_STATUS"

        private val _installEvents = MutableSharedFlow<InstallBroadcastEvent>(extraBufferCapacity = 8)
        val installEvents: SharedFlow<InstallBroadcastEvent> = _installEvents.asSharedFlow()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val otherPkg = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                _installEvents.tryEmit(InstallBroadcastEvent.Success)
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                _installEvents.tryEmit(InstallBroadcastEvent.SignatureMismatch(otherPkg ?: ""))
            }
            else -> {
                _installEvents.tryEmit(InstallBroadcastEvent.Failure(message ?: "Installation failed (code $status)"))
            }
        }
    }
}

sealed interface InstallBroadcastEvent {
    data object Success : InstallBroadcastEvent
    data class SignatureMismatch(val packageName: String) : InstallBroadcastEvent
    data class Failure(val message: String) : InstallBroadcastEvent
}
