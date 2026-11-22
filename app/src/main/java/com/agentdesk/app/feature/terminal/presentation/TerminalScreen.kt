package com.agentdesk.app.feature.terminal.presentation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentdesk.app.feature.terminal.data.TermuxSessionHandle
import com.agentdesk.app.feature.terminal.presentation.components.ExtraKeysBar
import com.agentdesk.app.feature.terminal.presentation.components.TerminalViewHost
import com.agentdesk.app.feature.terminal.presentation.components.TermuxContextMenuDialog
import com.agentdesk.app.feature.terminal.presentation.components.TermuxDrawer
import com.agentdesk.app.feature.terminal.presentation.components.TermuxRenameDialog
import com.agentdesk.app.feature.terminal.presentation.components.TermuxUrlDialog
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val activeSession = state.sessions.getOrNull(state.selectedSessionIndex)
    val termSession = (activeSession?.handle as? TermuxSessionHandle)?.terminalSession
    val drawerState = rememberDrawerState(
        initialValue = if (state.isDrawerOpen) DrawerValue.Open else DrawerValue.Closed
    )
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? Activity

    // Handle keep screen on
    DisposableEffect(state.keepScreenOn) {
        if (state.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Sync drawer state with ViewModel
    LaunchedEffect(state.isDrawerOpen) {
        if (state.isDrawerOpen && drawerState.isClosed) {
            drawerState.open()
        } else if (!state.isDrawerOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen != state.isDrawerOpen) {
            viewModel.onEvent(TerminalEvent.SetDrawerOpen(drawerState.isOpen))
        }
    }

    // Collect UI Effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TerminalEffect.ShareText -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Terminal Transcript"))
                }
                is TerminalEffect.OpenBrowser -> {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(effect.url))
                        context.startActivity(browserIntent)
                    } catch (ignored: Exception) {}
                }
                else -> {}
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TermuxDrawer(
                sessions = state.sessions,
                selectedIndex = state.selectedSessionIndex,
                onSelectSession = { viewModel.onEvent(TerminalEvent.SelectSession(it)) },
                onNewSession = { viewModel.onEvent(TerminalEvent.NewSession) },
                onNewFailsafeSession = { viewModel.onEvent(TerminalEvent.NewFailsafeSession) },
                onCloseSession = { viewModel.onEvent(TerminalEvent.CloseSession(it)) },
                onRenameSession = { viewModel.onEvent(TerminalEvent.PromptRenameSession(it)) },
                onToggleKeyboard = { viewModel.onEvent(TerminalEvent.ToggleKeyboard) }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Minimalist Immersive Termux Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFF101010))
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drawer",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = activeSession?.title ?: "Termux",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )

                    IconButton(onClick = { viewModel.onEvent(TerminalEvent.ToggleKeyboard) }) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Toggle Keyboard",
                            tint = Color(0xFFBBBBBB)
                        )
                    }

                    IconButton(onClick = { viewModel.onEvent(TerminalEvent.OpenContextMenu) }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color(0xFFBBBBBB)
                        )
                    }

                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF888888)
                        )
                    }
                }

                // Authentic Termux Terminal View
                TerminalViewHost(
                    terminalSession = termSession,
                    terminalViewClient = viewModel.viewClient,
                    modifier = Modifier.weight(1f)
                )

                // Termux 2-Row Extra Keys Bar
                ExtraKeysBar(
                    onKeyPress = { viewModel.onEvent(TerminalEvent.ExtraKeyPressed(it)) },
                    isCtrlActive = state.isCtrlActive,
                    isAltActive = state.isAltActive
                )
            }

            // Context Menu Dialog
            TermuxContextMenuDialog(
                isOpen = state.isContextMenuOpen,
                keepScreenOn = state.keepScreenOn,
                onDismiss = { viewModel.onEvent(TerminalEvent.DismissContextMenu) },
                onSelectUrl = { viewModel.onEvent(TerminalEvent.SelectUrl) },
                onShareTranscript = { viewModel.onEvent(TerminalEvent.ShareTranscript) },
                onResetTerminal = { viewModel.onEvent(TerminalEvent.ClearCurrentSession) },
                onKillProcess = { viewModel.onEvent(TerminalEvent.KillCurrentProcess) },
                onToggleKeepScreenOn = { viewModel.onEvent(TerminalEvent.ToggleKeepScreenOn) }
            )

            // URL Dialog
            TermuxUrlDialog(
                isOpen = state.showUrlDialog,
                urls = state.detectedUrls,
                onDismiss = { viewModel.onEvent(TerminalEvent.DismissUrlDialog) },
                onUrlSelected = { url ->
                    viewModel.onEvent(TerminalEvent.DismissUrlDialog)
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(browserIntent)
                    } catch (ignored: Exception) {}
                }
            )

            // Rename Dialog
            TermuxRenameDialog(
                isOpen = state.renameSessionIndex != null,
                currentTitle = state.renameInputText,
                onTitleChange = { viewModel.onEvent(TerminalEvent.RenameInputChanged(it)) },
                onConfirm = { newTitle ->
                    state.renameSessionIndex?.let { idx ->
                        viewModel.onEvent(TerminalEvent.ConfirmRenameSession(idx, newTitle))
                    }
                },
                onDismiss = { viewModel.onEvent(TerminalEvent.DismissRenameDialog) }
            )
        }
    }
}
