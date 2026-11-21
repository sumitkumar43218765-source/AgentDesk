package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.ai.domain.ToolRegistry
import com.agentdesk.app.feature.ai.domain.model.ToolSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistryImpl @Inject constructor(
    private val listDirTool: ListDirTool,
    private val readFileTool: ReadFileTool,
    private val searchFilesTool: SearchFilesTool,
    private val writeFileTool: WriteFileTool,
    private val editFileTool: EditFileTool,
    private val deletePathTool: DeletePathTool,
    private val shellExecTool: ShellExecTool,
    private val buildApkTool: BuildApkTool,
    private val readBuildLogTool: ReadBuildLogTool,
    private val installApkTool: InstallApkTool,
    private val gitTool: GitTool,
    private val askUserTool: AskUserTool
) : ToolRegistry {

    private val tools: List<AgentTool> by lazy {
        listOf(
            listDirTool,
            readFileTool,
            searchFilesTool,
            writeFileTool,
            editFileTool,
            deletePathTool,
            shellExecTool,
            buildApkTool,
            readBuildLogTool,
            installApkTool,
            gitTool,
            askUserTool
        )
    }

    private val toolMap: Map<String, AgentTool> by lazy {
        tools.associateBy { it.spec.name }
    }

    override fun getTools(): List<ToolSpec> {
        return tools.map { it.spec }
    }

    fun getTool(name: String): AgentTool? {
        return toolMap[name]
    }

    fun getAllAgentTools(): List<AgentTool> {
        return tools
    }
}
