package com.agentdesk.app.feature.terminal

import android.content.Context
import com.agentdesk.app.feature.terminal.data.DefaultTerminalViewClient
import com.agentdesk.app.feature.terminal.data.FakeShellBackend
import com.agentdesk.app.feature.terminal.presentation.TerminalEvent
import com.agentdesk.app.feature.terminal.presentation.TerminalViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeShellBackend: FakeShellBackend
    private lateinit var context: Context
    private lateinit var viewClient: DefaultTerminalViewClient
    private lateinit var viewModel: TerminalViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        viewClient = DefaultTerminalViewClient(context)
        fakeShellBackend = FakeShellBackend()
        viewModel = TerminalViewModel(fakeShellBackend, viewClient)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialSession_isCreatedOnInit() = runTest {
        assertEquals(1, viewModel.currentState.sessions.size)
        assertEquals("Session 1", viewModel.currentState.sessions[0].title)
        assertEquals(0, viewModel.currentState.selectedSessionIndex)
    }

    @Test
    fun newSession_createsSecondTabAndSelectsIt() = runTest {
        viewModel.onEvent(TerminalEvent.NewSession)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.currentState.sessions.size)
        assertEquals("Session 2", viewModel.currentState.sessions[1].title)
        assertEquals(1, viewModel.currentState.selectedSessionIndex)
    }

    @Test
    fun selectSession_switchesActiveTab() = runTest {
        viewModel.onEvent(TerminalEvent.NewSession)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.currentState.selectedSessionIndex)

        viewModel.onEvent(TerminalEvent.SelectSession(0))
        assertEquals(0, viewModel.currentState.selectedSessionIndex)
    }

    @Test
    fun closeSession_removesSessionAndAdjustsIndex() = runTest {
        viewModel.onEvent(TerminalEvent.NewSession)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.currentState.sessions.size)

        viewModel.onEvent(TerminalEvent.CloseSession(1))
        assertEquals(1, viewModel.currentState.sessions.size)
        assertEquals(0, viewModel.currentState.selectedSessionIndex)
    }

    @Test
    fun extraKey_ctrlAndAlt_togglesModifiers() = runTest {
        assertFalse(viewModel.currentState.isCtrlActive)
        assertFalse(viewModel.currentState.isAltActive)

        viewModel.onEvent(TerminalEvent.ExtraKeyPressed("CTRL"))
        assertTrue(viewModel.currentState.isCtrlActive)
        assertTrue(viewClient.isControlActive())

        viewModel.onEvent(TerminalEvent.ExtraKeyPressed("ALT"))
        assertTrue(viewModel.currentState.isAltActive)
        assertTrue(viewClient.isAltActive())

        // Toggle back off
        viewModel.onEvent(TerminalEvent.ExtraKeyPressed("CTRL"))
        assertFalse(viewModel.currentState.isCtrlActive)
        assertFalse(viewClient.isControlActive())
    }
}
