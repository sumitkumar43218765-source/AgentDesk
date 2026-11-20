package com.agentdesk.app.feature.setup.data.manifest

import android.content.Context
import com.agentdesk.app.core.common.AppError
import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.setup.domain.model.ToolDefinition
import com.agentdesk.app.feature.setup.domain.repository.ToolCatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetToolCatalogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: ToolManifestParser
) : ToolCatalogRepository {

    private val _catalogFlow = MutableStateFlow<List<ToolDefinition>>(emptyList())
    private val mutex = Mutex()
    private var cachedList: List<ToolDefinition>? = null

    override fun getCatalog(): Flow<List<ToolDefinition>> = _catalogFlow.asStateFlow()

    override suspend fun loadCatalog(): AppResult<List<ToolDefinition>> = mutex.withLock {
        cachedList?.let { return AppResult.Success(it) }

        try {
            val content = context.assets.open("tool_manifest.json").use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
            val result = parser.parse(content)
            if (result is AppResult.Success) {
                cachedList = result.value
                _catalogFlow.value = result.value
            }
            result
        } catch (e: Exception) {
            AppResult.Failure(
                AppError.Storage(
                    cause = e,
                    message = "Failed to read tool_manifest.json from assets: ${e.message}"
                )
            )
        }
    }

    override suspend fun getTool(id: String): ToolDefinition? {
        if (cachedList == null) {
            loadCatalog()
        }
        return cachedList?.firstOrNull { it.id == id }
    }
}
