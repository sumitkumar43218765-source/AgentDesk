package com.agentdesk.app.feature.ai.data.http

import com.agentdesk.app.core.common.AppError
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorMapper @Inject constructor() {

    fun map(httpCode: Int, responseBody: String?): AppError {
        val parsedMessage = extractMessage(responseBody)

        return when (httpCode) {
            401, 403 -> AppError.Auth(parsedMessage ?: "Authentication failed: invalid API key or unauthorized")
            429 -> {
                val retryAfterMs = extractRetryAfterMs(responseBody)
                AppError.RateLimit(parsedMessage ?: "Rate limit exceeded. Please wait before retrying.", retryAfterMs)
            }
            in 500..599 -> AppError.Network(parsedMessage ?: "Remote AI server error ($httpCode)")
            else -> {
                if (parsedMessage?.contains("context_length_exceeded", ignoreCase = true) == true ||
                    parsedMessage?.contains("prompt is too long", ignoreCase = true) == true
                ) {
                    AppError.Unknown("CONTEXT_TOO_LONG: $parsedMessage")
                } else {
                    AppError.Unknown(parsedMessage ?: "Request failed with HTTP status $httpCode")
                }
            }
        }
    }

    private fun extractMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = JSONObject(body)
            when {
                json.has("error") -> {
                    val errorObj = json.get("error")
                    if (errorObj is JSONObject) {
                        errorObj.optString("message", null) ?: errorObj.toString()
                    } else {
                        errorObj.toString()
                    }
                }
                json.has("message") -> json.getString("message")
                else -> body.take(200)
            }
        } catch (e: Exception) {
            body.take(200)
        }
    }

    private fun extractRetryAfterMs(body: String?): Long? {
        if (body == null) return null
        return try {
            val json = JSONObject(body)
            if (json.has("retry_after")) {
                json.getLong("retry_after") * 1000L
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
