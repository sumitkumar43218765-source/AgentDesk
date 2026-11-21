package com.agentdesk.app.feature.build.data.parse

import com.agentdesk.app.feature.build.domain.model.BuildDiagnostic
import com.agentdesk.app.feature.build.domain.model.BuildStepId
import com.agentdesk.app.feature.build.domain.model.Severity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EcjOutputParser @Inject constructor() {

    private val headerRegex = Regex("""^(\d+)\.\s+(ERROR|WARNING)\s+in\s+(.+?)\s+\(at line (\d+)\)$""")
    private val caretRegex = Regex("""^(\s*)(\^+)\s*$""")

    fun parse(rawOutput: String, projectRootPath: String = ""): List<BuildDiagnostic> {
        val diagnostics = mutableListOf<BuildDiagnostic>()
        val lines = rawOutput.lines()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            val headerMatch = headerRegex.matchEntire(line)

            if (headerMatch != null) {
                val sevStr = headerMatch.groupValues[2]
                val rawFile = headerMatch.groupValues[3].trim()
                val lineNum = headerMatch.groupValues[4].toIntOrNull()

                val severity = if (sevStr.equals("ERROR", ignoreCase = true)) {
                    Severity.ERROR
                } else {
                    Severity.WARNING
                }

                val relativeFile = cleanPath(rawFile, projectRootPath)

                // Seek next lines for source snippet, caret (column), and message
                var column: Int? = null
                val messageBuilder = StringBuilder()
                var j = i + 1

                while (j < lines.size && !lines[j].trim().startsWith("----------") && !headerRegex.matches(lines[j].trim())) {
                    val curr = lines[j]
                    val caretMatch = caretRegex.matchEntire(curr)
                    if (caretMatch != null) {
                        column = caretMatch.groupValues[1].length + 1
                    } else if (!curr.trim().startsWith("setContentView") && curr.trim().isNotEmpty()) {
                        // After caret line, lines are the error message
                        if (column != null) {
                            if (messageBuilder.isNotEmpty()) messageBuilder.append(" ")
                            messageBuilder.append(curr.trim())
                        }
                    }
                    j++
                }

                val finalMessage = if (messageBuilder.isNotEmpty()) {
                    messageBuilder.toString()
                } else {
                    "Compilation issue in $relativeFile"
                }

                diagnostics.add(
                    BuildDiagnostic(
                        severity = severity,
                        file = relativeFile,
                        line = lineNum,
                        column = column,
                        message = finalMessage,
                        step = BuildStepId.JAVA_COMPILE
                    )
                )

                i = j - 1
            }
            i++
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
