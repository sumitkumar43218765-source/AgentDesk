package com.agentdesk.app.feature.terminal.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

class DefaultTerminalSessionClient(
    private val context: Context,
    private val onTitleUpdate: ((TerminalSession, String) -> Unit)? = null,
    private val onSessionExit: ((TerminalSession) -> Unit)? = null
) : TerminalSessionClient {

    private val clipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    override fun onTextChanged(changedSession: TerminalSession) {
        // Redraw is handled by attached TerminalView
    }

    override fun onTitleChanged(changedSession: TerminalSession) {
        val title = changedSession.title ?: "Terminal"
        onTitleUpdate?.invoke(changedSession, title)
    }

    override fun onSessionFinished(finishedSession: TerminalSession) {
        onSessionExit?.invoke(finishedSession)
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        clipboardManager?.setPrimaryClip(ClipData.newPlainText("Terminal", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession) {
        val clip = clipboardManager?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).coerceToText(context).toString()
            if (text.isNotEmpty()) {
                session.write(text)
            }
        }
    }

    override fun onBell(session: TerminalSession) {
        // Bell callback
    }

    override fun onColorsChanged(session: TerminalSession) {}

    override fun onTerminalCursorStateChange(state: Boolean) {}

    override fun getTerminalCursorStyle(): Int? = null

    override fun logError(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun logWarn(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun logVerbose(tag: String, message: String) {
        Log.v(tag, message)
    }

    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
        Log.e(tag, message, e)
    }

    override fun logStackTrace(tag: String, e: Exception) {
        Log.e(tag, "TerminalSession exception", e)
    }
}
