package com.agentdesk.app.feature.agent.domain.model

import com.agentdesk.app.feature.build.domain.model.BuildRun
import kotlinx.serialization.json.JsonObject

sealed interface AgentEvent {
    data class StepStarted(val stepIndex: Int) : AgentEvent
    data class AssistantText(val delta: String) : AgentEvent
    data class ToolRequested(val toolCallId: String, val toolName: String, val args: JsonObject) : AgentEvent
    data class PermissionRequested(val request: PermissionRequest) : AgentEvent
    data class PermissionResolved(val requestId: String, val decision: PermissionDecision) : AgentEvent
    data class ToolStarted(val toolCallId: String, val toolName: String) : AgentEvent
    data class ToolOutputPartial(val toolCallId: String, val chunk: String) : AgentEvent
    data class ToolFinished(val toolCallId: String, val toolName: String, val output: ToolOutput) : AgentEvent
    data class BuildStarted(val buildId: String) : AgentEvent
    data class BuildFinished(val run: BuildRun) : AgentEvent
    data class Error(val message: String, val fatal: Boolean) : AgentEvent
    data class Completed(val finalAnswer: String) : AgentEvent
    data object Cancelled : AgentEvent
}
