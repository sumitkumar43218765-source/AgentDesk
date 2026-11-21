package com.agentdesk.app.feature.projects.data.git

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.GitCommit
import com.agentdesk.app.feature.projects.domain.model.GitStatus
import com.agentdesk.app.feature.projects.domain.repository.GitService
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import com.agentdesk.app.feature.terminal.domain.PathProvider
import com.agentdesk.app.feature.terminal.domain.ShellBackend
import com.agentdesk.app.feature.terminal.domain.ShellEnvironment
import com.agentdesk.app.feature.terminal.domain.ShellRequest
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShellGitService @Inject constructor(
    private val shellBackend: ShellBackend,
    private val pathProvider: PathProvider,
    private val fileSystem: ProjectFileSystem,
    private val parser: GitOutputParser,
    private val askpassWriter: AskpassScriptWriter
) : GitService {

    private suspend fun runGit(
        projectId: String,
        command: String,
        executionId: String
    ): AppResult<String> {
        val rootDir = fileSystem.root(projectId)
        val guestCwd = fileSystem.guestPath(projectId)

        val fullCommand = "git -C \"$guestCwd\" $command"
        val request = ShellRequest(
            command = fullCommand,
            cwd = guestCwd,
            environment = ShellEnvironment.PROOT_UBUNTU,
            executionId = executionId
        )

        val result = shellBackend.runToCompletion(request)
        return if (result.exitCode == 0) {
            AppResult.Success(result.stdout)
        } else {
            AppResult.Failure(
                AppError.Shell(
                    message = "git command failed ($command): ${result.stderr.ifEmpty { result.stdout }}",
                    exitCode = result.exitCode
                )
            )
        }
    }

    override suspend fun init(projectId: String): AppResult<Unit> {
        val guestCwd = fileSystem.guestPath(projectId)
        val initCmd = "git init -b main && git config user.name \"Agent\" && git config user.email \"agent@localhost\""
        val res = runGit(projectId, "init -b main", "git_init_$projectId")
        if (res is AppResult.Failure) return res

        runGit(projectId, "config user.name \"Agent\"", "git_cfg_name_$projectId")
        runGit(projectId, "config user.email \"agent@localhost\"", "git_cfg_email_$projectId")

        // Initial commit
        checkpoint(projectId, "Initial commit from template")
        return AppResult.Success(Unit)
    }

    override suspend fun status(projectId: String): AppResult<GitStatus> {
        val res = runGit(projectId, "status --porcelain=v1 -b", "git_status_$projectId")
        return when (res) {
            is AppResult.Success -> AppResult.Success(parser.parseStatus(res.value))
            is AppResult.Failure -> res
        }
    }

    override suspend fun checkpoint(projectId: String, message: String): AppResult<GitCommit?> {
        val statusRes = status(projectId)
        if (statusRes is AppResult.Success && statusRes.value.clean) {
            return AppResult.Success(null)
        }

        val addRes = runGit(projectId, "add -A", "git_add_$projectId")
        if (addRes is AppResult.Failure) return addRes

        val escapedMessage = message.replace("\"", "\\\"")
        val commitRes = runGit(projectId, "commit -m \"$escapedMessage\"", "git_commit_$projectId")
        if (commitRes is AppResult.Failure) return commitRes

        val logRes = log(projectId, 1)
        return when (logRes) {
            is AppResult.Success -> AppResult.Success(logRes.value.firstOrNull())
            is AppResult.Failure -> AppResult.Success(null)
        }
    }

    override suspend fun log(projectId: String, limit: Int): AppResult<List<GitCommit>> {
        val format = "%H|%h|%an|%at|%s"
        val res = runGit(projectId, "log -n $limit --format=\"$format\"", "git_log_$projectId")
        return when (res) {
            is AppResult.Success -> AppResult.Success(parser.parseLog(res.value))
            is AppResult.Failure -> res
        }
    }

    override suspend fun diffStat(projectId: String, from: String?): AppResult<String> {
        val cmd = if (from != null) "diff --stat $from" else "diff --stat HEAD"
        return runGit(projectId, cmd, "git_diff_$projectId")
    }

    override suspend fun restore(projectId: String, commit: String): AppResult<Unit> {
        // Create safety commit first
        checkpoint(projectId, "Safety checkpoint before restoring $commit")

        val resetRes = runGit(projectId, "reset --hard $commit", "git_restore_$projectId")
        return when (resetRes) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> resetRes
        }
    }

    override suspend fun setRemote(projectId: String, url: String?): AppResult<Unit> {
        if (url == null) {
            runGit(projectId, "remote remove origin", "git_rem_rm_$projectId")
        } else {
            val removeRes = runGit(projectId, "remote remove origin", "git_rem_rm_$projectId")
            val addRes = runGit(projectId, "remote add origin \"$url\"", "git_rem_add_$projectId")
            if (addRes is AppResult.Failure) return addRes
        }
        return AppResult.Success(Unit)
    }

    override suspend fun push(projectId: String): AppResult<Unit> {
        val res = runGit(projectId, "push -u origin main", "git_push_$projectId")
        return when (res) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Failure -> res
        }
    }
}
