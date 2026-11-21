package com.agentdesk.app.feature.agent.data.tools

import com.agentdesk.app.feature.agent.domain.AgentTool
import com.agentdesk.app.feature.agent.domain.AgentToolRegistry
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
) : AgentToolRegistry {

    private val agentTools: List<AgentTool> by lazy {
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
        agentTools.associateBy { it.spec.name }
    }

    override fun getTools(): List<ToolSpec> {
        return agentTools.map { it.spec }
    }

    override fun getTool(name: String): AgentTool? {
        return toolMap[name]
    }

    override fun getAllAgentTools(): List<AgentTool> {
        return agentTools
    }
}
