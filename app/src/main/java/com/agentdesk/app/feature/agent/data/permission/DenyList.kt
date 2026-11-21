package com.agentdesk.app.feature.agent.data.permission

object DenyList {

    private val deniedPatterns = listOf(
        Regex("""(?i)\brm\s+-[a-z]*r[a-z]*f?[a-z]*\s+/(?:\s|$)"""),
        Regex("""(?i)\brm\s+-[a-z]*r[a-z]*f?[a-z]*\s+~(?:\s|/|$)"""),
        Regex("""(?i)\brm\s+-[a-z]*r[a-z]*f?[a-z]*\s+\${'$'}HOME(?:\s|/|$)"""),
        Regex(""":\(\)\s*\{\s*:\s*\|\s*:\s*&\s*\}\s*;\s*:"""), // fork bomb
        Regex("""(?i)\bmkfs(?:\.[a-z0-9]+)?\b"""),
        Regex("""(?i)\bdd\s+if=/dev/(?:zero|urandom)\s+of=/dev/"""),
        Regex("""(?i)\bshutdown\b"""),
        Regex("""(?i)\breboot\b""")
    )

    fun isDenied(command: String): Boolean {
        val trimmed = command.trim()
        return deniedPatterns.any { it.containsMatchIn(trimmed) }
    }
}
