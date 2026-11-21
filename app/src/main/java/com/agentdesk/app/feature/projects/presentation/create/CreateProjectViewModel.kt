package com.agentdesk.app.feature.projects.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.data.fs.ProjectSlugger
import com.agentdesk.app.feature.projects.domain.model.Project
import com.agentdesk.app.feature.projects.domain.model.TemplateId
import com.agentdesk.app.feature.projects.domain.usecase.CreateProjectUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateProjectUiState(
    val name: String = "",
    val slug: String = "",
    val packageName: String = "",
    val template: TemplateId = TemplateId.HELLO_JAVA,
    val isCreating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateProjectViewModel @Inject constructor(
    private val createProjectUseCase: CreateProjectUseCase,
    private val slugger: ProjectSlugger
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateProjectUiState())
    val uiState = _uiState.asStateFlow()

    private val _projectCreated = MutableSharedFlow<Project>()
    val projectCreated = _projectCreated.asSharedFlow()

    fun onNameChanged(newName: String) {
        val slug = slugger.generateSlug(newName, emptySet())
        val pkg = slugger.derivePackageName(slug)
        _uiState.update {
            it.copy(
                name = newName,
                slug = slug,
                packageName = pkg,
                error = null
            )
        }
    }

    fun onPackageNameChanged(newPkg: String) {
        _uiState.update { it.copy(packageName = newPkg) }
    }

    fun onTemplateSelected(template: TemplateId) {
        _uiState.update { it.copy(template = template) }
    }

    fun createProject() {
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "Project name cannot be empty") }
            return
        }

        _uiState.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            val result = createProjectUseCase(
                name = name,
                template = state.template,
                packageName = state.packageName.ifBlank { null }
            )
            if (result is AppResult.Success) {
                _projectCreated.emit(result.value)
            } else if (result is AppResult.Failure) {
                _uiState.update { it.copy(isCreating = false, error = result.error.message) }
            }
        }
    }
}
