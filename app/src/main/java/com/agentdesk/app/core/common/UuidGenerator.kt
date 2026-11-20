package com.agentdesk.app.core.common

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UuidGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
