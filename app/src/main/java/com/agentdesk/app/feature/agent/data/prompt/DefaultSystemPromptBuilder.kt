package com.agentdesk.app.feature.agent.data.prompt

import android.content.Context
import com.agentdesk.app.R
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.agent.domain.SystemPromptBuilder
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import com.agentdesk.app.feature.projects.domain.repository.ProjectRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSystemPromptBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val environmentFacts: EnvironmentFacts,
    private val projectRepository: ProjectRepository,
    private val projectFileSystem: ProjectFileSystem
) : SystemPromptBuilder {

    override suspend fun build(projectId: String): String {
        val basePrompt = loadBasePrompt()
        val envSummary = environmentFacts.getEnvironmentSummary()

        val project = projectRepository.get(projectId)
        val projectSection = buildString {
            append("## Current Project\n")
            if (project != null) {
                append("- Project Name: ${project.name}\n")
                append("- Package Name: ${project.packageName}\n")
                append("- Template: ${project.template}\n")
            } else {
                append("- Project ID: $projectId\n")
            }

            append("\n### Project Structure\n")
            when (val listRes = projectFileSystem.list(projectId, "", 1)) {
                is AppResult.Success -> {
                    val entries = listRes.value
                    if (entries.isEmpty()) {
                        append("(empty project root)\n")
                    } else {
                        for (entry in entries) {
                            val prefix = if (entry.isDirectory) "[DIR]" else "[FILE]"
                            append("- $prefix ${entry.relativePath}\n")
                        }
                    }
                }
                is AppResult.Failure -> {
                    append("- (Failed to list files: ${listRes.error.message})\n")
                }
            }
        }

        val toolGuide = """
            ## Available Tools Quick Reference
            - list_dir: Explore directory contents.
            - read_file: View file contents with line numbers.
            - search_files: Find text patterns in project files.
            - write_file: Create or completely overwrite a file.
            - edit_file: Targeted string replacement (requires exact match).
            - delete_path: Remove a file or folder.
            - shell_exec: Run command in project directory inside proot.
            - build_apk: Compile and package the Android APK.
            - read_build_log: Page through build logs if build_apk fails.
            - install_apk: Request installation of built APK on device.
            - git: Manage version control (status, diff, log, commit, restore, push).
            - ask_user: Ask clarifying questions when needed.
        """.trimIndent()

        return buildString {
            append(basePrompt.trim())
            append("\n\n")
            append(envSummary.trim())
            append("\n\n")
            append(projectSection.trim())
            append("\n\n")
            append(toolGuide.trim())
        }
    }

    private fun loadBasePrompt(): String {
        return try {
            context.resources.openRawResource(R.raw.prompt_agent_system).use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).readText()
            }
        } catch (e: Exception) {
            "You are AgentDesk, an autonomous Android developer assistant."
        }
    }
}
