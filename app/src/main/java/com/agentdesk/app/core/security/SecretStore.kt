package com.agentdesk.app.core.security

interface SecretStore {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String)
    suspend fun contains(key: String): Boolean
}
