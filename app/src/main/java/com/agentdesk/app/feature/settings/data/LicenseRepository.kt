package com.agentdesk.app.feature.settings.data

import com.agentdesk.app.feature.settings.domain.model.LicenseInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepository @Inject constructor() {

    fun getLicenses(): List<LicenseInfo> {
        return listOf(
            LicenseInfo(
                library = "AgentDesk Core & Termux Fork",
                license = "GPL-3.0",
                sourceUrl = "https://github.com/sumitkumar43218765-source/AgentDesk",
                description = "Autonomous AI Agent Desktop for Android with embedded PRoot environment."
            ),
            LicenseInfo(
                library = "Termux App & Packages",
                license = "GPL-3.0",
                sourceUrl = "https://github.com/termux/termux-app",
                description = "Android terminal emulator and Linux environment."
            ),
            LicenseInfo(
                library = "Kotlin & Android Jetpack / Compose",
                license = "Apache-2.0",
                sourceUrl = "https://android.googlesource.com/",
                description = "Modern UI toolkit, Room persistence, and architecture components."
            ),
            LicenseInfo(
                library = "Dagger Hilt",
                license = "Apache-2.0",
                sourceUrl = "https://github.com/google/dagger",
                description = "Dependency injection for Android."
            ),
            LicenseInfo(
                library = "OkHttp & Kotlinx Serialization",
                license = "Apache-2.0",
                sourceUrl = "https://square.github.io/okhttp/",
                description = "HTTP client, SSE streaming, and JSON serialization."
            )
        )
    }
}
