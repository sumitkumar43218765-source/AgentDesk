package com.agentdesk.app.feature.agent.domain.usecase

import com.agentdesk.app.feature.agent.domain.model.UserAnswer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnswerAskUserUseCase @Inject constructor() {

    private val _answers = MutableSharedFlow<UserAnswer>(extraBufferCapacity = 16)
    val answers: SharedFlow<UserAnswer> = _answers.asSharedFlow()

    suspend operator fun invoke(questionId: String, answer: String) {
        _answers.emit(UserAnswer(questionId, answer))
    }
}
