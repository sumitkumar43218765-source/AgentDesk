package com.agentdesk.app.feature.build.domain.usecase

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.ApkInstaller
import com.agentdesk.app.feature.build.domain.model.InstallOutcome
import java.io.File
import javax.inject.Inject

class InstallApkUseCase @Inject constructor(
    private val apkInstaller: ApkInstaller
) {
    suspend operator fun invoke(apkHostPath: String): AppResult<InstallOutcome> {
        val file = File(apkHostPath)
        if (!file.exists()) {
            return AppResult.Failure(AppError.Storage("APK file does not exist: $apkHostPath"))
        }
        return apkInstaller.install(file)
    }
}
