package com.agentdesk.app.feature.setup.data.install

import com.agentdesk.app.feature.setup.domain.model.InstallPlan
import com.agentdesk.app.feature.terminal.domain.PathProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallScriptGenerator @Inject constructor(
    private val pathProvider: PathProvider,
    private val doneMarkers: DoneMarkers
) {

    fun generateScript(plan: InstallPlan): String {
        val statusFilePath = File(pathProvider.setupStateDir, "status.jsonl").absolutePath
        val script = StringBuilder()

        script.appendLine("#!/bin/bash")
        script.appendLine("set -e")
        script.appendLine("STATUS_FILE=\"$statusFilePath\"")
        script.appendLine("mkdir -p \"$(dirname \"$STATUS_FILE\")\"")
        script.appendLine()
        script.appendLine("log_event() {")
        script.appendLine("    local tool=\"$1\"")
        script.appendLine("    local event=\"$2\"")
        script.appendLine("    local pct=\"$3\"")
        script.appendLine("    local msg=\"$4\"")
        script.appendLine("    local ts=\$(date +%s%3N)")
        script.appendLine("    echo \"{\\\"ts\\\":\$ts,\\\"tool\\\":\\\"\$tool\\\",\\\"event\\\":\\\"\$event\\\",\\\"pct\\\":\$pct,\\\"msg\\\":\\\"\$msg\\\"}\" >> \"\$STATUS_FILE\"")
        script.appendLine("}")
        script.appendLine()
        script.appendLine("retry() {")
        script.appendLine("    local max=3; local n=1; local delay=2")
        script.appendLine("    while true; do")
        script.appendLine("        if \"$@\"; then break; else")
        script.appendLine("            if [ \$n -lt \$max ]; then")
        script.appendLine("                n=\$((n+1))")
        script.appendLine("                echo \"Step failed. Retrying \$n/\$max in \${delay}s...\"")
        script.appendLine("                sleep \$delay")
        script.appendLine("                delay=\$((delay*2))")
        script.appendLine("            else")
        script.appendLine("                echo \"Step failed permanently after \$max attempts.\"")
        script.appendLine("                return 1")
        script.appendLine("            fi")
        script.appendLine("        fi")
        script.appendLine("    done")
        script.appendLine("}")
        script.appendLine()

        for (tool in plan.toolsToInstall) {
            val doneMarker = doneMarkers.getDoneMarkerPath(tool.id, tool.version)
            script.appendLine("# --- Tool: ${tool.name} (${tool.id}) ---")
            script.appendLine("mkdir -p \"$(dirname \"$doneMarker\")\"")
            script.appendLine("if [ -f \"$doneMarker\" ] && ${tool.verify} >/dev/null 2>&1; then")
            script.appendLine("    log_event \"${tool.id}\" \"skip\" 100 \"Already installed and verified\"")
            script.appendLine("    echo \"[SKIP] ${tool.name} is already installed.\"")
            script.appendLine("else")
            script.appendLine("    log_event \"${tool.id}\" \"start\" 0 \"Starting installation\"")
            script.appendLine("    echo \"[START] Installing ${tool.name}...\"")
            script.appendLine("    set +e")

            for (cmd in tool.install) {
                script.appendLine("    echo \"Executing: $cmd\"")
                script.appendLine("    retry $cmd")
                script.appendLine("    CMD_EXIT=\$?")
                script.appendLine("    if [ \$CMD_EXIT -ne 0 ]; then")
                script.appendLine("        log_event \"${tool.id}\" \"fail\" 0 \"Command failed: $cmd\"")
                script.appendLine("        echo \"[FAIL] Installation failed for ${tool.name}\"")
                if (tool.required) {
                    script.appendLine("        exit 1")
                } else {
                    script.appendLine("        continue")
                }
                script.appendLine("    fi")
            }

            script.appendLine("    log_event \"${tool.id}\" \"progress\" 90 \"Verifying installation\"")
            script.appendLine("    echo \"Verifying with: ${tool.verify}\"")
            script.appendLine("    ${tool.verify}")
            script.appendLine("    VERIFY_EXIT=\$?")
            script.appendLine("    if [ \$VERIFY_EXIT -eq 0 ]; then")
            script.appendLine("        touch \"$doneMarker\"")
            script.appendLine("        log_event \"${tool.id}\" \"ok\" 100 \"Verified successfully\"")
            script.appendLine("        echo \"[OK] ${tool.name} verified successfully.\"")
            script.appendLine("    else")
            script.appendLine("        log_event \"${tool.id}\" \"fail\" 0 \"Verification failed: ${tool.verify}\"")
            script.appendLine("        echo \"[FAIL] Verification failed for ${tool.name}\"")
            if (tool.required) {
                script.appendLine("        exit 1")
            }
            script.appendLine("    fi")
            script.appendLine("    set -e")
            script.appendLine("fi")
            script.appendLine()
        }

        script.appendLine("echo \"All installation steps completed.\"")
        return script.toString()
    }
}
