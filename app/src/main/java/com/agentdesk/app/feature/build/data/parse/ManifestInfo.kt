package com.agentdesk.app.feature.build.data.parse

data class ManifestInfo(
    val packageName: String,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val versionCode: Int,
    val versionName: String
)
