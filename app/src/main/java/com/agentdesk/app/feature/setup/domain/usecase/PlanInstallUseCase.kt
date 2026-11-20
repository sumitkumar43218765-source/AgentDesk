package com.agentdesk.app.feature.setup.domain.usecase

import android.content.Context
import android.os.StatFs
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.InstallPlanner
import com.agentdesk.app.feature.setup.domain.model.InstallPlan
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PlanInstallUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalogRepository: ToolCatalogRepository,
    private val planner: InstallPlanner
) {
    suspend operator fun invoke(selectedToolIds: Set<String>): AppResult<InstallPlan> {
        val catalogResult = catalogRepository.loadCatalog()
        if (catalogResult !is AppResult.Success) {
            return AppResult.Failure(AppError.Storage(message = "Failed to load catalog for planning"))
        }

        val stat = StatFs(context.filesDir.absolutePath)
        val availableMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)

        return planner.plan(catalogResult.value, selectedToolIds, availableMb)
    }
}
