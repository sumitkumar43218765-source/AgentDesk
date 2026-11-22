package com.agentdesk.app.feature.terminal

import android.content.Context
import com.agentdesk.app.feature.terminal.data.DefaultTerminalViewClient
import com.agentdesk.app.feature.terminal.data.FakeShellBackend
import com.agentdesk.app.feature.terminal.presentation.TerminalEffect
import com.agentdesk.app.feature.terminal.presentation.TerminalEvent
import com.agentdesk.app.feature.terminal.presentation.TerminalViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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
        assertFalse(viewModel.currentState.sessions[0].isFailsafe)
    }

    @Test
    fun newSession_createsSecondTabAndSelectsIt() = runTest {
        viewModel.onEvent(TerminalEvent.NewSession)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.currentState.sessions.size)
        assertEquals("Session 2", viewModel.currentState.sessions[1].title)
        assertEquals(1, viewModel.currentState.selectedSessionIndex)
        assertFalse(viewModel.currentState.sessions[1].isFailsafe)
    }

    @Test
    fun newFailsafeSession_createsFailsafeTab() = runTest {
        viewModel.onEvent(TerminalEvent.NewFailsafeSession)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.currentState.sessions.size)
        assertTrue(viewModel.currentState.sessions[1].isFailsafe)
        assertEquals("Failsafe 2", viewModel.currentState.sessions[1].title)
    }

    @Test
    fun renameSession_updatesSessionTitle() = runTest {
        viewModel.onEvent(TerminalEvent.PromptRenameSession(0))
        assertEquals(0, viewModel.currentState.renameSessionIndex)
        assertEquals("Session 1", viewModel.currentState.renameInputText)

        viewModel.onEvent(TerminalEvent.RenameInputChanged("My Root Shell"))
        assertEquals("My Root Shell", viewModel.currentState.renameInputText)

        viewModel.onEvent(TerminalEvent.ConfirmRenameSession(0, "My Root Shell"))
        assertEquals("My Root Shell", viewModel.currentState.sessions[0].title)
        assertEquals(null, viewModel.currentState.renameSessionIndex)
    }

    @Test
    fun toggleDrawer_opensAndClosesDrawer() = runTest {
        assertFalse(viewModel.currentState.isDrawerOpen)

        viewModel.onEvent(TerminalEvent.ToggleDrawer)
        assertTrue(viewModel.currentState.isDrawerOpen)

        viewModel.onEvent(TerminalEvent.SetDrawerOpen(false))
        assertFalse(viewModel.currentState.isDrawerOpen)
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

    @Test
    fun contextMenu_openAndDismiss() = runTest {
        assertFalse(viewModel.currentState.isContextMenuOpen)

        viewModel.onEvent(TerminalEvent.OpenContextMenu)
        assertTrue(viewModel.currentState.isContextMenuOpen)

        viewModel.onEvent(TerminalEvent.DismissContextMenu)
        assertFalse(viewModel.currentState.isContextMenuOpen)
    }

    @Test
    fun toggleKeepScreenOn_updatesState() = runTest {
        assertFalse(viewModel.currentState.keepScreenOn)

        viewModel.onEvent(TerminalEvent.ToggleKeepScreenOn)
        assertTrue(viewModel.currentState.keepScreenOn)

        viewModel.onEvent(TerminalEvent.ToggleKeepScreenOn)
        assertFalse(viewModel.currentState.keepScreenOn)
    }
}
