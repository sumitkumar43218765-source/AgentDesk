package com.agentdesk.app.feature.setup.domain.usecase

import android.content.Context
import android.content.Intent
import android.os.Build
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.data.install.InstallScriptGenerator
import com.agentdesk.app.feature.setup.data.install.SetupInstallService
import com.agentdesk.app.feature.setup.data.install.StatusTailer
import com.agentdesk.app.feature.setup.domain.model.InstallPlan
import com.agentdesk.app.feature.setup.domain.model.ToolInstallStatus
import com.agentdesk.app.feature.setup.domain.repository.ToolStateRepository
import com.agentdesk.app.feature.terminal.domain.PathProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class StartInstallUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scriptGenerator: InstallScriptGenerator,
    private val pathProvider: PathProvider,
    private val stateRepository: ToolStateRepository,
    private val statusTailer: StatusTailer
) {
    suspend operator fun invoke(plan: InstallPlan): AppResult<Unit> {
        if (!plan.hasEnoughSpace) {
            return AppResult.Failure(
                AppError.Storage(
                    message = "Insufficient disk space. Shortfall: ${plan.spaceShortfallMb} MB"
                )
            )
        }

        val scriptContent = scriptGenerator.generateScript(plan)
        val scriptFile = File(pathProvider.setupStateDir, "run_install.sh")
        scriptFile.parentFile?.mkdirs()
        scriptFile.writeText(scriptContent)
        scriptFile.setExecutable(true, false)

        statusTailer.clear()

        for (tool in plan.toolsToInstall) {
            stateRepository.setToolStatus(tool.id, ToolInstallStatus.QUEUED)
        }

        val intent = Intent(context, SetupInstallService::class.java).apply {
            action = SetupInstallService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        return AppResult.Success(Unit)
    }
}
