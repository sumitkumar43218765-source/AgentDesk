package com.agentdesk.app.feature.build.data.parse

import com.agentdesk.app.feature.build.domain.model.BuildDiagnostic
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.Severity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Aapt2OutputParser @Inject constructor() {

    // Matches: path/to/file.xml:12: error: message. OR path/to/file.xml:12:34: error: message.
    private val lineColRegex = Regex("""^(.+?):(\d+)(?::(\d+))?:\s*(error|warning):\s*(.+)$""")
    // Matches: error: message. OR warning: message.
    private val genericRegex = Regex("""^(error|warning):\s*(.+)$""")

    fun parse(rawOutput: String, step: BuildStepId, projectRootPath: String = ""): List<BuildDiagnostic> {
        val diagnostics = mutableListOf<BuildDiagnostic>()
        val lines = rawOutput.lines()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val matchWithFile = lineColRegex.matchEntire(line)
            if (matchWithFile != null) {
                val rawFile = matchWithFile.groupValues[1].trim()
                val lineNum = matchWithFile.groupValues[2].toIntOrNull()
                val colNum = matchWithFile.groupValues[3].toIntOrNull()
                val sevStr = matchWithFile.groupValues[4]
                val msg = matchWithFile.groupValues[5].trim()

                val severity = if (sevStr.equals("error", ignoreCase = true)) {
                    Severity.ERROR
                } else {
                    Severity.WARNING
                }

                diagnostics.add(
                    BuildDiagnostic(
                        severity = severity,
                        file = cleanPath(rawFile, projectRootPath),
                        line = lineNum,
                        column = colNum,
                        message = msg,
                        step = step
                    )
                )
                continue
            }

            val genericMatch = genericRegex.matchEntire(line)
            if (genericMatch != null) {
                val sevStr = genericMatch.groupValues[1]
                val msg = genericMatch.groupValues[2].trim()

                val severity = if (sevStr.equals("error", ignoreCase = true)) {
                    Severity.ERROR
                } else {
                    Severity.WARNING
                }

                diagnostics.add(
                    BuildDiagnostic(
                        severity = severity,
                        file = null,
                        line = null,
                        column = null,
                        message = msg,
                        step = step
                    )
                )
            }
        }

        return diagnostics
    }

    private fun cleanPath(filePath: String, root: String): String {
        var path = filePath
        if (root.isNotEmpty()) {
            val rootNorm = root.trimEnd('/')
            if (path.startsWith(rootNorm)) {
                path = path.removePrefix(rootNorm).trimStart('/')
            }
        }
        val guestPrefix = "/root/projects/"
        if (path.startsWith(guestPrefix)) {
            val withoutPrefix = path.removePrefix(guestPrefix)
            val slashIdx = withoutPrefix.indexOf('/')
            if (slashIdx >= 0) {
                path = withoutPrefix.substring(slashIdx + 1)
            }
        }
        return path
    }
}
