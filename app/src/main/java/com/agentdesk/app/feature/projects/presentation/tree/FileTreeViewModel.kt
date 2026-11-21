package com.agentdesk.app.feature.projects.presentation.tree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.projects.domain.model.FileEntry
import com.agentdesk.app.feature.projects.domain.repository.ProjectFileSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileTreeUiState(
    val entries: List<FileEntry> = emptyList(),
    val expandedPaths: Set<String> = emptySet(),
    val hideGitAndBuild: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FileTreeViewModel @Inject constructor(
    private val fileSystem: ProjectFileSystem
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileTreeUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var currentProjectId: String? = null

    fun loadTree(projectId: String) {
        currentProjectId = projectId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val res = fileSystem.list(projectId, "", depth = 1)
            if (res is AppResult.Success) {
                _uiState.update { it.copy(entries = filterEntries(res.value, it.hideGitAndBuild), isLoading = false) }
            } else if (res is AppResult.Failure) {
                _uiState.update { it.copy(isLoading = false, error = res.error.message) }
            }
        }
    }

    fun toggleDirectory(entry: FileEntry) {
        if (!entry.isDirectory) return
        val projectId = currentProjectId ?: return

        val expanded = _uiState.value.expandedPaths.toMutableSet()
        if (expanded.contains(entry.relativePath)) {
            expanded.remove(entry.relativePath)
            _uiState.update { it.copy(expandedPaths = expanded) }
        } else {
            expanded.add(entry.relativePath)
            _uiState.update { it.copy(expandedPaths = expanded) }
            // Fetch child entries
            viewModelScope.launch {
                val res = fileSystem.list(projectId, entry.relativePath, depth = 1)
                if (res is AppResult.Success) {
                    val currentList = _uiState.value.entries.toMutableList()
                    val parentIdx = currentList.indexOfFirst { it.relativePath == entry.relativePath }
                    if (parentIdx != -1) {
                        val children = filterEntries(res.value, _uiState.value.hideGitAndBuild)
                        currentList.addAll(parentIdx + 1, children)
                        _uiState.update { it.copy(entries = currentList.distinctBy { e -> e.relativePath }) }
                    }
                }
            }
        }
    }

    fun toggleHideGitAndBuild() {
        val next = !_uiState.value.hideGitAndBuild
        _uiState.update { it.copy(hideGitAndBuild = next) }
        currentProjectId?.let { loadTree(it) }
    }

    private fun filterEntries(list: List<FileEntry>, hide: Boolean): List<FileEntry> {
        if (!hide) return list
        return list.filter {
            !it.name.startsWith(".git") && !it.name.startsWith("build")
        }
    }
}
