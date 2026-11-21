package com.agentdesk.app.feature.terminal.data

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTerminalViewClient @Inject constructor(
    @ApplicationContext private val context: Context
) : TerminalViewClient {

    private var terminalView: TerminalView? = null
    private var controlActive: Boolean = false
    private var altActive: Boolean = false
    private var currentTextSizeSp: Float = 14f
    var onFontSizeChanged: ((Int) -> Unit)? = null

    fun attachView(view: TerminalView) {
        this.terminalView = view
    }

    fun setControlActive(active: Boolean) {
        this.controlActive = active
    }

    fun isControlActive(): Boolean = controlActive

    fun setAltActive(active: Boolean) {
        this.altActive = active
    }

    fun isAltActive(): Boolean = altActive

    override fun onScale(scale: Float): Float {
        currentTextSizeSp = (currentTextSizeSp * scale).coerceIn(8f, 32f)
        terminalView?.setTextSize(currentTextSizeSp.toInt())
        onFontSizeChanged?.invoke(currentTextSizeSp.toInt())
        return currentTextSizeSp
    }

    override fun onSingleTapUp(e: MotionEvent) {
        terminalView?.let { view ->
            view.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false

    override fun shouldEnforceCharBasedInput(): Boolean = true

    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

    override fun isTerminalViewSelected(): Boolean = true

    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false

    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false

    override fun onLongPress(event: MotionEvent): Boolean = false

    override fun readControlKey(): Boolean {
        val wasActive = controlActive
        controlActive = false
        return wasActive
    }

    override fun readAltKey(): Boolean {
        val wasActive = altActive
        altActive = false
        return wasActive
    }

    override fun readShiftKey(): Boolean = false

    override fun readFnKey(): Boolean = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false

    override fun onEmulatorSet() {
        terminalView?.updateSize()
    }

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
        Log.e(tag, "TerminalView exception", e)
    }
}
