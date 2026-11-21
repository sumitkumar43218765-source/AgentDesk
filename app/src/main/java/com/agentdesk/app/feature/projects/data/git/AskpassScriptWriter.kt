package com.agentdesk.app.feature.projects.data.git

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AskpassScriptWriter @Inject constructor() {

    fun createAskpassScript(dir: File, token: String): File {
        val script = File(dir, "askpass.sh")
        script.writeText(
            """
            #!/bin/sh
            echo "$token"
            """.trimIndent()
        )
        script.setExecutable(true, false)
        return script
    }
}
