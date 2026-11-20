package com.agentdesk.app.feature.setup.data.install

import com.agentdesk.app.feature.setup.domain.model.SetupStatusEvent
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusLineParser @Inject constructor() {

    fun parseLine(line: String): SetupStatusEvent? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null
        }
        return try {
            val json = JSONObject(trimmed)
            SetupStatusEvent(
                ts = json.optLong("ts", System.currentTimeMillis()),
                tool = json.optString("tool", ""),
                event = json.optString("event", "unknown"),
                pct = json.optInt("pct", 0),
                msg = json.optString("msg", "")
            )
        } catch (_: Exception) {
            null
        }
    }
}
