package com.agentdesk.app.core.common

sealed interface AppError {
    val message: String

    data class Network(override val message: String, val cause: Throwable? = null) : AppError
    data class Auth(override val message: String) : AppError
    data class RateLimit(override val message: String, val retryAfterMs: Long? = null) : AppError
    data class Storage(override val message: String, val cause: Throwable? = null) : AppError
    data class Shell(override val message: String, val exitCode: Int? = null) : AppError
    data class Build(override val message: String) : AppError
    data class Permission(override val message: String) : AppError
    data class Cancelled(override val message: String = "Cancelled") : AppError
    data class Unknown(override val message: String, val cause: Throwable? = null) : AppError
}
