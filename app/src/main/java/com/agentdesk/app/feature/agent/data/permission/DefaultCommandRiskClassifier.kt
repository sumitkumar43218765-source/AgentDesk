package com.agentdesk.app.feature.agent.data.permission

import com.agentdesk.app.feature.agent.domain.CommandRiskClassifier
import com.agentdesk.app.feature.agent.domain.model.RiskLevel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCommandRiskClassifier @Inject constructor() : CommandRiskClassifier {

    private val highRiskPatterns = listOf(
        Regex("""(?i)\bcurl\b.*\|\s*(?:bash|sh)"""),
        Regex("""(?i)\bwget\b.*\|\s*(?:bash|sh)"""),
        Regex("""(?i)\bsudo\b"""),
        Regex("""(?i)\bchmod\s+-[a-z]*R"""),
        Regex("""(?i)\bchown\s+-[a-z]*R"""),
        Regex("""(?i)\brm\s+-[a-z]*r"""), // recursive rm
        Regex("""(?i)\bmv\s+.*(?:\s+/|\s+~)"""),
        Regex("""(?i)\bexport\s+LD_PRELOAD""")
    )

    override fun classify(command: String, cwd: String): RiskLevel {
        val trimmed = command.trim()
        if (DenyList.isDenied(trimmed)) {
            return RiskLevel.DENIED
        }

        if (highRiskPatterns.any { it.containsMatchIn(trimmed) }) {
            return RiskLevel.HIGH
        }

        if (ShellAllowlist.isAllowed(trimmed)) {
            return RiskLevel.LOW
        }

        // Modifying commands
        if (trimmed.startsWith("rm ") || trimmed.startsWith("cp ") ||
            trimmed.startsWith("mv ") || trimmed.startsWith("sed ") ||
            trimmed.startsWith("git reset") || trimmed.startsWith("git checkout")
        ) {
            return RiskLevel.MEDIUM
        }

        return RiskLevel.LOW
    }
}
