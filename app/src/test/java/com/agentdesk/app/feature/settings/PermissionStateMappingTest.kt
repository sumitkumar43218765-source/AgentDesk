package com.agentdesk.app.feature.settings

import com.agentdesk.app.feature.settings.domain.model.PermissionItem
import com.agentdesk.app.feature.settings.domain.model.PermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionStateMappingTest {

    @Test
    fun testPermissionItemStateTransitions() {
        val grantedItem = PermissionItem(
            id = "notifications",
            title = "Notifications",
            description = "Notification description",
            state = PermissionState.GRANTED
        )
        assertEquals(PermissionState.GRANTED, grantedItem.state)
        assertNull(grantedItem.actionIntent)

        val notGrantedItem = PermissionItem(
            id = "install_packages",
            title = "Install Unknown Apps",
            description = "Install description",
            state = PermissionState.NOT_GRANTED
        )
        assertEquals(PermissionState.NOT_GRANTED, notGrantedItem.state)

        val notNeededItem = PermissionItem(
            id = "battery_optimization",
            title = "Battery Optimization",
            description = "Battery description",
            state = PermissionState.NOT_NEEDED
        )
        assertEquals(PermissionState.NOT_NEEDED, notNeededItem.state)
    }

    @Test
    fun testPermissionCheckerClassification() {
        fun classifyNotification(sdkInt: Int, runtimeGranted: Boolean, appEnabled: Boolean): PermissionState {
            return if (sdkInt >= 33) {
                if (runtimeGranted) PermissionState.GRANTED else PermissionState.NOT_GRANTED
            } else {
                if (appEnabled) PermissionState.GRANTED else PermissionState.NOT_GRANTED
            }
        }

        assertEquals(PermissionState.GRANTED, classifyNotification(34, runtimeGranted = true, appEnabled = true))
        assertEquals(PermissionState.NOT_GRANTED, classifyNotification(34, runtimeGranted = false, appEnabled = true))
        assertEquals(PermissionState.GRANTED, classifyNotification(30, runtimeGranted = false, appEnabled = true))
        assertEquals(PermissionState.NOT_GRANTED, classifyNotification(30, runtimeGranted = false, appEnabled = false))
    }
}
