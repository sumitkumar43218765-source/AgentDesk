package com.agentdesk.app.feature.projects.data.template

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRenderer @Inject constructor() {

    fun render(
        content: String,
        packageName: String,
        appLabel: String,
        minSdk: String = "24",
        targetSdk: String = "34"
    ): String {
        return content
            .replace("\${PACKAGE}", packageName)
            .replace("\${APP_LABEL}", appLabel)
            .replace("\${MIN_SDK}", minSdk)
            .replace("\${TARGET_SDK}", targetSdk)
    }
}
