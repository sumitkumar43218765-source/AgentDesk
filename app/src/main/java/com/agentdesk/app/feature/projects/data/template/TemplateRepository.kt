package com.agentdesk.app.feature.projects.data.template

import android.content.Context
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.TemplateId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val renderer: TemplateRenderer
) {

    fun populateProject(
        targetDir: File,
        template: TemplateId,
        packageName: String,
        appLabel: String
    ): AppResult<Unit> {
        targetDir.mkdirs()

        try {
            when (template) {
                TemplateId.EMPTY -> {
                    copyAsset("templates/empty/.gitignore", File(targetDir, ".gitignore"))
                }
                TemplateId.HELLO_JAVA -> {
                    // 1. AndroidManifest.xml
                    renderAsset(
                        "templates/hello-java/AndroidManifest.xml",
                        File(targetDir, "AndroidManifest.xml"),
                        packageName,
                        appLabel
                    )
                    // 2. res/layout/activity_main.xml
                    renderAsset(
                        "templates/hello-java/res/layout/activity_main.xml",
                        File(targetDir, "res/layout/activity_main.xml"),
                        packageName,
                        appLabel
                    )
                    // 3. res/values/strings.xml
                    renderAsset(
                        "templates/hello-java/res/values/strings.xml",
                        File(targetDir, "res/values/strings.xml"),
                        packageName,
                        appLabel
                    )
                    // 4. src/<package-path>/MainActivity.java
                    val packageDir = packageName.replace('.', '/')
                    val javaTarget = File(targetDir, "src/$packageDir/MainActivity.java")
                    renderAsset(
                        "templates/hello-java/src/MainActivity.java",
                        javaTarget,
                        packageName,
                        appLabel
                    )
                    // 5. .gitignore
                    copyAsset("templates/hello-java/.gitignore", File(targetDir, ".gitignore"))
                }
                TemplateId.HELLO_JNI -> {
                    // Future expansion
                    copyAsset("templates/empty/.gitignore", File(targetDir, ".gitignore"))
                }
            }
            return AppResult.Success(Unit)
        } catch (e: Exception) {
            return AppResult.Failure(
                AppError.Storage(
                    cause = e,
                    message = "Failed to render template ${template.name}: ${e.message}"
                )
            )
        }
    }

    private fun copyAsset(assetPath: String, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun renderAsset(
        assetPath: String,
        targetFile: File,
        packageName: String,
        appLabel: String
    ) {
        targetFile.parentFile?.mkdirs()
        val raw = context.assets.open(assetPath).use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        }
        val rendered = renderer.render(raw, packageName, appLabel)
        targetFile.writeText(rendered)
    }
}
