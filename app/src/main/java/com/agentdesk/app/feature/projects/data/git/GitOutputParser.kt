package com.agentdesk.app.feature.projects.data.git

import com.agentdesk.app.feature.projects.domain.model.GitCommit
import com.agentdesk.app.feature.projects.domain.model.GitStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitOutputParser @Inject constructor() {

    fun parseStatus(output: String): GitStatus {
        val lines = output.lines()
        var branch = "main"
        var staged = 0
        var unstaged = 0
        var untracked = 0

        for (line in lines) {
            if (line.startsWith("##")) {
                val branchPart = line.removePrefix("##").trim()
                branch = branchPart.split("...").firstOrNull() ?: "main"
            } else if (line.length >= 2) {
                val indexStatus = line[0]
                val workTreeStatus = line[1]

                if (indexStatus == '?' && workTreeStatus == '?') {
                    untracked++
                } else {
                    if (indexStatus != ' ' && indexStatus != '?') staged++
                    if (workTreeStatus != ' ' && workTreeStatus != '?') unstaged++
                }
            }
        }

        val clean = (staged == 0 && unstaged == 0 && untracked == 0)
        return GitStatus(
            branch = branch,
            clean = clean,
            stagedCount = staged,
            unstagedCount = unstaged,
            untrackedCount = untracked
        )
    }

    fun parseLog(output: String): List<GitCommit> {
        if (output.isBlank()) return emptyList()
        val commits = mutableListOf<GitCommit>()

        // Format used: %H|%h|%an|%at|%s
        val lines = output.lines()
        for (line in lines) {
            val parts = line.split('|')
            if (parts.size >= 5) {
                val hash = parts[0]
                val shortHash = parts[1]
                val author = parts[2]
                val timestampSec = parts[3].toLongOrNull() ?: (System.currentTimeMillis() / 1000)
                val message = parts.subList(4, parts.size).joinToString("|")

                commits.add(
                    GitCommit(
                        hash = hash,
                        shortHash = shortHash,
                        author = author,
                        timestamp = timestampSec * 1000,
                        message = message
                    )
                )
            }
        }
        return commits
    }
}
