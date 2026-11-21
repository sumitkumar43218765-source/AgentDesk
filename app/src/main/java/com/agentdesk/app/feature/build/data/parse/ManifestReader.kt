package com.agentdesk.app.feature.build.data.parse

import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

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
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var packageName: String? = null
            var minSdk = 21
            var targetSdk = 28
            var versionCode = 1
            var versionName = "1.0"

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "manifest" -> {
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)
                                if (attrName == "package") {
                                    packageName = parser.getAttributeValue(i)
                                } else if (attrName == "versionCode") {
                                    parser.getAttributeValue(i).toIntOrNull()?.let { versionCode = it }
                                } else if (attrName == "versionName") {
                                    versionName = parser.getAttributeValue(i)
                                }
                            }
                        }
                        "uses-sdk" -> {
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)
                                if (attrName == "minSdkVersion") {
                                    parser.getAttributeValue(i).toIntOrNull()?.let { minSdk = it }
                                } else if (attrName == "targetSdkVersion") {
                                    parser.getAttributeValue(i).toIntOrNull()?.let { targetSdk = it }
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            // Fallback for package regex if parser missed it due to namespace quirks
            if (packageName.isNullOrBlank()) {
                val pkgRegex = Regex("""package\s*=\s*["']([^"']+)["']""")
                packageName = pkgRegex.find(xmlContent)?.groupValues?.get(1)
            }

            if (packageName.isNullOrBlank()) {
                return AppResult.Failure(AppError.Build("Missing or empty 'package' attribute in AndroidManifest.xml"))
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
