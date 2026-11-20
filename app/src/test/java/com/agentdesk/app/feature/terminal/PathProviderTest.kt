package com.agentdesk.app.feature.terminal

import android.content.Context
import com.agentdesk.app.feature.terminal.data.DefaultPathProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PathProviderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var pathProvider: DefaultPathProvider

    @Before
    fun setUp() {
        context = mockk()
        every { context.filesDir } returns tempFolder.root
        pathProvider = DefaultPathProvider(context)
    }

    @Test
    fun hostToGuest_insideRootfs_mapsCorrectly() {
        val hostFile = File(pathProvider.rootfsDir, "root/projects/demo/MainActivity.java")
        val guest = pathProvider.hostToGuest(hostFile)
        assertEquals("/root/projects/demo/MainActivity.java", guest)
    }

    @Test
    fun hostToGuest_outsideRootfs_returnsNull() {
        val hostFile = File(tempFolder.root, "logs/app.log")
        val guest = pathProvider.hostToGuest(hostFile)
        assertNull(guest)
    }

    @Test
    fun guestToHost_mapsToRootfsSubpath() {
        val host = pathProvider.guestToHost("/root/projects/demo")
        val expected = File(pathProvider.rootfsDir, "root/projects/demo")
        assertEquals(expected.absolutePath, host.absolutePath)
    }
}
