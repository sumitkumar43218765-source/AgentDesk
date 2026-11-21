package com.agentdesk.app.feature.agent.data.permission

object ShellAllowlist {

    private val exactAllowlist = setOf(
        "ls", "pwd", "which", "whoami",
        "java -version", "javac -version", "ecj -version",
        "aapt2 version", "d8 --version", "git --version",
        "python3 --version", "node -v", "npm -v"
    )

    private val prefixAllowlist = listOf(
        "ls ", "cat ", "head ", "tail ", "wc ",
        "echo ", "which ", "file ", "find ",
        "git status", "git diff", "git log", "git branch", "git show"
    )

    fun isAllowed(command: String): Boolean {
        val trimmed = command.trim()
        if (exactAllowlist.contains(trimmed)) return true
        return prefixAllowlist.any { trimmed.startsWith(it) && !trimmed.contains(";") && !trimmed.contains("|") && !trimmed.contains("&&") }
    }
}
