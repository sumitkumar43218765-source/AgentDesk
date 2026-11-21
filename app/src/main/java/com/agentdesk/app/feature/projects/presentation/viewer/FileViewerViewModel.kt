package com.agentdesk.app.feature.projects.presentation.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.FileContent
import com.agentdesk.app.feature.projects.domain.repository.AskAgentAboutFile
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileViewerUiState(
    val projectId: String = "",
    val relativePath: String = "",
    val fileContent: FileContent? = null,
    val highlightedText: AnnotatedString = AnnotatedString(""),
    val jumpLine: Int? = null,
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class FileViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileSystem: ProjectFileSystem,
    private val highlighter: SyntaxHighlighter,
    private val askAgent: AskAgentAboutFile,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val projectId: String = savedStateHandle["projectId"] ?: ""
    private val relativePath: String = savedStateHandle["path"] ?: ""
    private val initialLine: Int? = savedStateHandle["line"]

    private val _uiState = MutableStateFlow(
        FileViewerUiState(
            projectId = projectId,
            relativePath = relativePath,
            jumpLine = initialLine
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadFile()
    }

    private fun loadFile() {
        if (projectId.isEmpty() || relativePath.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // 512 KB cap = 524_288 bytes
            val res = fileSystem.read(projectId, relativePath, maxBytes = 524_288)
            if (res is AppResult.Success) {
                val content = res.value
                val highlighted = highlighter.highlight(content.content, relativePath)
                _uiState.update {
                    it.copy(
                        fileContent = content,
                        highlightedText = highlighted,
                        isLoading = false
                    )
                }
            } else if (res is AppResult.Failure) {
                _uiState.update { it.copy(isLoading = false, error = res.error.message) }
            }
        }
    }

    fun copyPath() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("File Path", relativePath))
        _uiState.update { it.copy(message = "Path copied: $relativePath") }
    }

    fun askAgentAboutFile() {
        askAgent.ask(projectId, relativePath, null)
        _uiState.update { it.copy(message = "Added file reference to agent chat") }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
