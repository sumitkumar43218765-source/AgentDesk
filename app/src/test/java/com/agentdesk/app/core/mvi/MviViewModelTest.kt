package com.agentdesk.app.core.mvi

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    data class TestState(val counter: Int = 0) : UiState
    sealed interface TestEvent : UiEvent {
        data object Increment : TestEvent
        data class ShowToast(val message: String) : TestEvent
    }
    sealed interface TestEffect : UiEffect {
        data class Toast(val message: String) : TestEffect
    }

    class TestViewModel : MviViewModel<TestState, TestEvent, TestEffect>(TestState()) {
        override fun onEvent(event: TestEvent) {
            when (event) {
                is TestEvent.Increment -> setState { copy(counter = counter + 1) }
                is TestEvent.ShowToast -> emitEffect(TestEffect.Toast(event.message))
            }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onEvent_increment_updatesState() {
        val viewModel = TestViewModel()
        assertEquals(0, viewModel.state.value.counter)

        viewModel.onEvent(TestEvent.Increment)
        assertEquals(1, viewModel.state.value.counter)
    }

    @Test
    fun onEvent_showToast_emitsEffect() = runTest {
        val viewModel = TestViewModel()
        viewModel.effects.test {
            viewModel.onEvent(TestEvent.ShowToast("Saved!"))
            testScheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertEquals(TestEffect.Toast("Saved!"), effect)
        }
    }
}
