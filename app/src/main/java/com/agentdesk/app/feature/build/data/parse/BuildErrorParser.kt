package com.agentdesk.app.feature.build.data.parse

import com.agentdesk.app.feature.build.domain.model.BuildDiagnostic
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildErrorParser @Inject constructor(
    private val ecjOutputParser: EcjOutputParser,
    private val aapt2OutputParser: Aapt2OutputParser
) {
    fun parse(step: BuildStepId, rawOutput: String, projectRootPath: String = ""): List<BuildDiagnostic> {
        return when (step) {
            BuildStepId.JAVA_COMPILE -> ecjOutputParser.parse(rawOutput, projectRootPath)
            BuildStepId.RESOURCE_COMPILE,
            BuildStepId.RESOURCE_LINK -> aapt2OutputParser.parse(rawOutput, step, projectRootPath)
            else -> emptyList()
        }
    }
}
