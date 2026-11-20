package com.agentdesk.app.feature.setup.domain

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.model.InstallPlan
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import javax.inject.Inject

class InstallPlanner @Inject constructor() {

    fun plan(
        allTools: List<ToolDefinition>,
        selectedToolIds: Set<String>,
        availableDiskSpaceMb: Long
    ): AppResult<InstallPlan> {
        val toolMap = allTools.associateBy { it.id }

        // Always include required tools and user selections
        val targets = mutableSetOf<String>()
        for (tool in allTools) {
            if (tool.required || selectedToolIds.contains(tool.id)) {
                targets.add(tool.id)
            }
        }

        // Resolve dependencies transitively
        val resolvedIds = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val orderedList = mutableListOf<ToolDefinition>()

        fun visit(toolId: String): AppResult<Unit> {
            if (visiting.contains(toolId)) {
                return AppResult.Failure(
                    AppError.Internal(
                        message = "Circular dependency detected involving tool: $toolId"
                    )
                )
            }
            if (!resolvedIds.contains(toolId)) {
                visiting.add(toolId)
                val tool = toolMap[toolId]
                if (tool != null) {
                    for (depId in tool.dependsOn) {
                        if (toolMap.containsKey(depId)) {
                            val depResult = visit(depId)
                            if (depResult is AppResult.Failure) {
                                return depResult
                            }
                        }
                    }
                    resolvedIds.add(toolId)
                    orderedList.add(tool)
                }
                visiting.remove(toolId)
            }
            return AppResult.Success(Unit)
        }

        for (targetId in targets) {
            val res = visit(targetId)
            if (res is AppResult.Failure) {
                return res
            }
        }

        val totalDownloadMb = orderedList.sumOf { it.sizeEstimateMb }
        // Required space = total * 1.5 + 500 MB
        val requiredDiskSpaceMb = (totalDownloadMb * 1.5).toLong() + 500L
        val hasEnoughSpace = availableDiskSpaceMb >= requiredDiskSpaceMb
        val shortfall = if (hasEnoughSpace) 0L else (requiredDiskSpaceMb - availableDiskSpaceMb)

        return AppResult.Success(
            InstallPlan(
                toolsToInstall = orderedList,
                totalDownloadMb = totalDownloadMb,
                requiredDiskSpaceMb = requiredDiskSpaceMb,
                availableDiskSpaceMb = availableDiskSpaceMb,
                hasEnoughSpace = hasEnoughSpace,
                spaceShortfallMb = shortfall
            )
        )
    }
}
