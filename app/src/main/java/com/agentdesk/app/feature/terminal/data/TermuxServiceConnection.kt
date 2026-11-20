package com.agentdesk.app.feature.terminal.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.agentdesk.app.termux.TermuxService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TermuxServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context
) : ServiceConnection {

    private val _service = MutableStateFlow<TermuxService?>(null)
    val service: StateFlow<TermuxService?> = _service.asStateFlow()

    fun bind() {
        val intent = Intent(context, TermuxService::class.java)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        try {
            context.unbindService(this)
            _service.value = null
        } catch (ignored: Exception) {}
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        if (binder is TermuxService.LocalBinder) {
            _service.value = binder.service
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        _service.value = null
    }
}
