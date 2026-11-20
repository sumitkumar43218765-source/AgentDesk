package com.agentdesk.app.feature.setup

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.data.manifest.ToolManifestParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToolManifestParserTest {

    private lateinit var parser: ToolManifestParser

    @Before
    fun setUp() {
        parser = ToolManifestParser()
    }

    @Test
    fun parse_validManifest_returnsToolList() {
        val json = """
            {
              "schemaVersion": 1,
              "tools": [
                {
                  "id": "jdk-17",
                  "name": "Java JDK 17",
                  "description": "Java runtime",
                  "category": "CORE",
                  "required": true,
                  "dependsOn": ["rootfs-ubuntu"],
                  "sizeEstimateMb": 300,
                  "environment": "PROOT_UBUNTU",
                  "source": { "kind": "APT", "packages": ["openjdk-17-jdk-headless"] },
                  "install": ["apt-get install -y openjdk-17-jdk-headless"],
                  "verify": "java -version",
                  "version": "17"
                }
              ]
            }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is AppResult.Success)
        val tools = (result as AppResult.Success).value
        assertEquals(1, tools.size)
        val tool = tools.first()
        assertEquals("jdk-17", tool.id)
        assertEquals("Java JDK 17", tool.name)
        assertTrue(tool.required)
        assertEquals(listOf("rootfs-ubuntu"), tool.dependsOn)
        assertEquals(300L, tool.sizeEstimateMb)
    }

    @Test
    fun parse_invalidJson_returnsFailure() {
        val result = parser.parse("not json")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun parse_unsupportedSchemaVersion_returnsFailure() {
        val json = """
            {
              "schemaVersion": 99,
              "tools": []
            }
        """.trimIndent()

        val result = parser.parse(json)
        assertTrue(result is AppResult.Failure)
    }
}
