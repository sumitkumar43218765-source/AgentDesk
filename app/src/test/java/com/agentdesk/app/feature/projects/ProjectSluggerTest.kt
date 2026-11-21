package com.agentdesk.app.feature.projects

import com.agentdesk.app.feature.projects.data.fs.ProjectSlugger
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProjectSluggerTest {

    private lateinit var slugger: ProjectSlugger

    @Before
    fun setUp() {
        slugger = ProjectSlugger()
    }

    @Test
    fun generateSlug_convertsToLowerCaseHyphenated() {
        val slug = slugger.generateSlug("My Awesome Android App!", emptySet())
        assertEquals("my-awesome-android-app", slug)
    }

    @Test
    fun generateSlug_handlesCollision() {
        val existing = setOf("calculator", "calculator-2")
        val slug = slugger.generateSlug("Calculator", existing)
        assertEquals("calculator-3", slug)
    }

    @Test
    fun derivePackageName_stripsDashes() {
        val pkg = slugger.derivePackageName("my-first-app")
        assertEquals("com.example.myfirstapp", pkg)
    }
}
