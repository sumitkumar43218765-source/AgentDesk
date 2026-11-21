package com.agentdesk.app.feature.build.data.parse

import com.agentdesk.app.feature.build.domain.model.DistilledLog
import kotlin.math.max
import kotlin.math.min

object LogDistiller {

    private val ansiRegex = Regex("""\u001B\[[0-9;]*[a-zA-Z]""")
    private val errorPattern = Regex("""(?i)(?:error|FAILED|Exception|fatal)""")
    private val warningDeprecatPattern = Regex("""(?i)warning:\s*.*deprecat""")

    fun distill(raw: String, maxChars: Int = 6000): DistilledLog {
        val clean = ansiRegex.replace(raw, "").replace("\r\n", "\n")
        val rawLines = clean.lines()
        val totalOriginalLines = rawLines.size

        if (clean.isBlank()) {
            return DistilledLog(
                text = "",
                totalOriginalLines = 0,
                omittedLinesCount = 0,
                errorCount = 0,
                warningCount = 0
            )
        }

        // 1. Collapse consecutive repeated lines
        val collapsedLines = mutableListOf<String>()
        var prevLine: String? = null
        var repeatCount = 0

        for (line in rawLines) {
            if (line == prevLine) {
                repeatCount++
            } else {
                if (repeatCount > 1) {
                    collapsedLines.add("  [Previous line repeated $repeatCount times]")
                }
                collapsedLines.add(line)
                prevLine = line
                repeatCount = 1
            }
        }
        if (repeatCount > 1) {
            collapsedLines.add("  [Previous line repeated $repeatCount times]")
        }

        val totalCollapsed = collapsedLines.size

        // 2. Scan for error and deprecation warning lines
        var errorCount = 0
        var warningCount = 0
        val importantIndices = mutableSetOf<Int>()

        for (i in 0 until totalCollapsed) {
            val line = collapsedLines[i]
            if (errorPattern.containsMatchIn(line)) {
                errorCount++
                importantIndices.add(i)
                // include 1 line context before/after if available
                if (i > 0) importantIndices.add(i - 1)
                if (i + 1 < totalCollapsed) importantIndices.add(i + 1)
            } else if (warningDeprecatPattern.containsMatchIn(line)) {
                warningCount++
                importantIndices.add(i)
            }
        }

        // 3. Keep first 20 and last 60 lines
        val headLimit = min(20, totalCollapsed)
        val tailStart = max(0, totalCollapsed - 60)

        for (i in 0 until headLimit) {
            importantIndices.add(i)
        }
        for (i in tailStart until totalCollapsed) {
            importantIndices.add(i)
        }

        val sortedIndices = importantIndices.sorted()

        // 4. Build output with omission markers
        val sb = StringBuilder()
        var omittedCount = 0
        var lastIdx = -1

        for (idx in sortedIndices) {
            if (lastIdx != -1 && idx > lastIdx + 1) {
                val gap = idx - lastIdx - 1
                omittedCount += gap
                sb.append("\n[... $gap lines omitted ...]\n")
            }
            sb.append(collapsedLines[idx]).append("\n")
            lastIdx = idx
        }

        var resultText = sb.toString().trimEnd()

        // 5. Cap at maxChars
        if (resultText.length > maxChars) {
            val truncatedMarker = "\n[... truncated to ${maxChars} chars ...]"
            val keepLength = max(0, maxChars - truncatedMarker.length)
            resultText = resultText.take(keepLength) + truncatedMarker
        }

        return DistilledLog(
            text = resultText,
            totalOriginalLines = totalOriginalLines,
            omittedLinesCount = omittedCount,
            errorCount = errorCount,
            warningCount = warningCount
        )
    }
}
