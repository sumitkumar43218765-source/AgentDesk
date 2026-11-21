package com.agentdesk.app.feature.build.domain.model

data class SigningConfig(
    val keystoreGuestPath: String,
    val storePass: String,
    val alias: String,
    val keyPass: String,
    val isDebug: Boolean
)
