package com.agentdesk.app.feature.chat.presentation.chat

import com.agentdesk.app.core.mvi.UiEffect

sealed interface ChatEffect : UiEffect {
    data class ShowToast(val message: String) : ChatEffect
    data object ScrollToBottom : ChatEffect
}
