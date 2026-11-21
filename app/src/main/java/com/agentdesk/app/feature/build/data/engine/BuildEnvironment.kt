package com.agentdesk.app.feature.build.data.engine

import com.agentdesk.app.feature.terminal.domain.PathProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildEnvironment @Inject constructor(
    private val pathProvider: PathProvider
) {
    fun resolveAndroidJar(): String {
        val candidates = listOf(
            "/usr/lib/android-sdk/platforms/android-34/android.jar",
            "/opt/android-sdk/platforms/android-34/android.jar",
            "/root/android-sdk/platforms/android-34/android.jar",
            "/usr/lib/android-sdk/platforms/android-33/android.jar"
        )
        for (guestPath in candidates) {
            val hostPath = pathProvider.guestToHost(guestPath)
            if (hostPath.exists()) return guestPath
        }
        return "/opt/android-sdk/platforms/android-34/android.jar"
    }

    fun resolveBuildTools(): String {
        val candidates = listOf(
            "/usr/lib/android-sdk/build-tools/34.0.0",
            "/opt/android-sdk/build-tools/34.0.0",
            "/usr/lib/android-sdk/build-tools/debian",
            "/usr/bin"
        )
        for (guestPath in candidates) {
            val hostPath = pathProvider.guestToHost(guestPath)
            if (hostPath.exists()) return guestPath
        }
        return "/usr/lib/android-sdk/build-tools/34.0.0"
    }

    fun resolveJavaHome(): String {
        val candidates = listOf(
            "/usr/lib/jvm/java-17-openjdk-arm64",
            "/usr/lib/jvm/java-17-openjdk",
            "/usr/lib/jvm/default-java"
        )
        for (guestPath in candidates) {
            val hostPath = pathProvider.guestToHost(guestPath)
            if (hostPath.exists()) return guestPath
        }
        return "/usr/lib/jvm/java-17-openjdk-arm64"
    }
}
