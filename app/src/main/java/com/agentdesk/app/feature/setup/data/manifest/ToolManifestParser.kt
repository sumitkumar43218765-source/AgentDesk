package com.agentdesk.app.feature.setup.data.manifest

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.model.ToolCategory
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.model.ToolSource
import com.agentdesk.app.feature.setup.domain.model.ToolSourceKind
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolManifestParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(jsonContent: String): AppResult<List<ToolDefinition>> {
        return try {
            val manifest = json.decodeFromString<ToolManifestDto>(jsonContent)
            if (manifest.schemaVersion != 1) {
                return AppResult.Failure(
                    AppError.Validation(
                        field = "schemaVersion",
                        message = "Unsupported manifest schemaVersion: ${manifest.schemaVersion}"
                    )
                )
            }
            val tools = manifest.tools.map { dto ->
                ToolDefinition(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    category = try {
                        ToolCategory.valueOf(dto.category)
                    } catch (_: Exception) {
                        ToolCategory.CORE
                    },
                    required = dto.required,
                    dependsOn = dto.dependsOn,
                    sizeEstimateMb = dto.sizeEstimateMb,
                    environment = try {
                        ShellEnvironment.valueOf(dto.environment)
                    } catch (_: Exception) {
                        ShellEnvironment.PROOT_UBUNTU
                    },
                    source = ToolSource(
                        kind = try {
                            ToolSourceKind.valueOf(dto.source.kind)
                        } catch (_: Exception) {
                            ToolSourceKind.APT
                        },
                        packages = dto.source.packages,
                        url = dto.source.url
                    ),
                    install = dto.install,
                    verify = dto.verify,
                    version = dto.version
                )
            }
            AppResult.Success(tools)
        } catch (e: Exception) {
            AppResult.Failure(
                AppError.Internal(
                    cause = e,
                    message = "Failed to parse tool manifest: ${e.message}"
                )
            )
        }
    }
}
