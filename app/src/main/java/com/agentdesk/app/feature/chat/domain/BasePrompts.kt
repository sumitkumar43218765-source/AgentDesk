package com.agentdesk.app.feature.chat.domain

import android.content.Context
import com.agentdesk.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BasePrompts @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getChatBasePrompt(): String {
        return try {
            val stream = context.resources.openRawResource(R.raw.prompt_chat_base)
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
        } catch (e: Exception) {
            "You are AgentDesk, an intelligent autonomous software development assistant on Android."
        }
    }
}
