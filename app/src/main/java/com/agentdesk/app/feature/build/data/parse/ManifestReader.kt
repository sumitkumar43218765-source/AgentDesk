package com.agentdesk.app.feature.build.data.parse

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

@Singleton
class ManifestReader @Inject constructor() {

    fun read(manifestFile: File): AppResult<ManifestInfo> {
        if (!manifestFile.exists()) {
            return AppResult.Failure(AppError.Storage("AndroidManifest.xml does not exist at ${manifestFile.absolutePath}"))
        }
        return readFromString(manifestFile.readText())
    }

    fun readFromString(xmlContent: String): AppResult<ManifestInfo> {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)))
            val root = doc.documentElement

            var packageName = root.getAttribute("package")
            if (packageName.isBlank()) {
                packageName = root.getAttributeNS("http://schemas.android.com/apk/res/android", "package")
            }
            if (packageName.isBlank()) {
                val pkgRegex = Regex("""package\s*=\s*["']([^"']+)["']""")
                packageName = pkgRegex.find(xmlContent)?.groupValues?.get(1) ?: ""
            }

            if (packageName.isBlank()) {
                return AppResult.Failure(AppError.Build("Missing or empty 'package' attribute in AndroidManifest.xml"))
            }

            var minSdk = 21
            var targetSdk = 28
            var versionCode = 1
            var versionName = "1.0"

            val vcStr = root.getAttributeNS("http://schemas.android.com/apk/res/android", "versionCode")
                .ifBlank { root.getAttribute("android:versionCode") }
            vcStr.toIntOrNull()?.let { versionCode = it }

            val vnStr = root.getAttributeNS("http://schemas.android.com/apk/res/android", "versionName")
                .ifBlank { root.getAttribute("android:versionName") }
            if (vnStr.isNotBlank()) versionName = vnStr

            val usesSdkList = root.getElementsByTagName("uses-sdk")
            if (usesSdkList.length > 0) {
                val sdkElem = usesSdkList.item(0) as? Element
                if (sdkElem != null) {
                    val minStr = sdkElem.getAttributeNS("http://schemas.android.com/apk/res/android", "minSdkVersion")
                        .ifBlank { sdkElem.getAttribute("android:minSdkVersion") }
                    minStr.toIntOrNull()?.let { minSdk = it }

                    val targetStr = sdkElem.getAttributeNS("http://schemas.android.com/apk/res/android", "targetSdkVersion")
                        .ifBlank { sdkElem.getAttribute("android:targetSdkVersion") }
                    targetStr.toIntOrNull()?.let { targetSdk = it }
                }
            }

            // Regex fallbacks in case DOM attributes didn't match namespace prefix
            if (minSdk == 21) {
                Regex("""android:minSdkVersion\s*=\s*["'](\d+)["']""").find(xmlContent)?.groupValues?.get(1)?.toIntOrNull()?.let { minSdk = it }
            }
            if (targetSdk == 28) {
                Regex("""android:targetSdkVersion\s*=\s*["'](\d+)["']""").find(xmlContent)?.groupValues?.get(1)?.toIntOrNull()?.let { targetSdk = it }
            }
            if (versionCode == 1) {
                Regex("""android:versionCode\s*=\s*["'](\d+)["']""").find(xmlContent)?.groupValues?.get(1)?.toIntOrNull()?.let { versionCode = it }
            }
            if (versionName == "1.0") {
                Regex("""android:versionName\s*=\s*["']([^"']+)["']""").find(xmlContent)?.groupValues?.get(1)?.let { versionName = it }
            }

            AppResult.Success(
                ManifestInfo(
                    packageName = packageName,
                    minSdkVersion = minSdk,
                    targetSdkVersion = targetSdk,
                    versionCode = versionCode,
                    versionName = versionName
                )
            )
        } catch (e: Exception) {
            AppResult.Failure(AppError.Build("Failed to parse AndroidManifest.xml: ${e.message}"))
        }
    }
}
