package com.agentdesk.app.feature.setup.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.app.navigation.Routes
import com.agentdesk.app.feature.setup.domain.usecase.ResolveStartRouteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val resolveStartRouteUseCase: ResolveStartRouteUseCase
) : ViewModel() {

    private val _targetRoute = MutableStateFlow<Routes?>(null)
    val targetRoute = _targetRoute.asStateFlow()

    init {
        resolveRoute()
    }

    fun resolveRoute() {
        viewModelScope.launch {
            val route = resolveStartRouteUseCase()
            _targetRoute.value = route
        }
    }
}
