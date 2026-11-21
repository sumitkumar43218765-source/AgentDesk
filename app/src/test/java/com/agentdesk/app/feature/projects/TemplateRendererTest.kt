package com.agentdesk.app.feature.projects

import com.agentdesk.app.feature.projects.data.template.TemplateRenderer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TemplateRendererTest {

    private lateinit var renderer: TemplateRenderer

    @Before
    fun setUp() {
        renderer = TemplateRenderer()
    }

    @Test
    fun render_replacesPlaceholdersCorrectly() {
        val template = """
            package ${'$'}{PACKAGE};
            // Label: ${'$'}{APP_LABEL}
            // SDK: ${'$'}{MIN_SDK} to ${'$'}{TARGET_SDK}
        """.trimIndent()

        val output = renderer.render(
            content = template,
            packageName = "com.example.test",
            appLabel = "Test App",
            minSdk = "26",
            targetSdk = "34"
        )

        val expected = """
            package com.example.test;
            // Label: Test App
            // SDK: 26 to 34
        """.trimIndent()

        assertEquals(expected, output)
    }
}
