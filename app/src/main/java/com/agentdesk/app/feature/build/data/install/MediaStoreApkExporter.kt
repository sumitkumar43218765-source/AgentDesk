package com.agentdesk.app.feature.build.data.install

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.domain.ApkExporter
import com.agentdesk.app.feature.build.domain.model.BuildRun
import com.agentdesk.app.feature.projects.data.local.ProjectDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreApkExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectDao: ProjectDao
) : ApkExporter {

    override suspend fun exportToDownloads(run: BuildRun): AppResult<Uri> = withContext(Dispatchers.IO) {
        val apkPath = run.apkHostPath
        if (apkPath.isNullOrBlank()) {
            return@withContext AppResult.Failure(AppError.Storage("No APK file found for build run ${run.id}"))
        }

        val sourceApk = File(apkPath)
        if (!sourceApk.exists()) {
            return@withContext AppResult.Failure(AppError.Storage("APK file does not exist at $apkPath"))
        }

        val project = projectDao.getById(run.projectId)
        val slug = project?.slug ?: "app"
        val versionName = "1.0"
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(run.startedAt))
        val targetFileName = "$slug-$versionName-$timestamp.apk"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/AgentDesk")
                }

                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext AppResult.Failure(AppError.Storage("Failed to create MediaStore entry in Downloads"))

                context.contentResolver.openOutputStream(uri)?.use { outStream ->
                    FileInputStream(sourceApk).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                } ?: return@withContext AppResult.Failure(AppError.Storage("Failed to write to MediaStore stream"))

                AppResult.Success(uri)
            } else {
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "AgentDesk"
                ).apply { if (!exists()) mkdirs() }

                val targetFile = File(downloadsDir, targetFileName)
                FileInputStream(sourceApk).use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
                AppResult.Success(fileUri)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.Storage("Failed to export APK: ${e.message}"))
        }
    }
}
