package com.agentdesk.app.feature.setup.data.install

import com.agentdesk.app.core.common.DispatcherProvider
import com.agentdesk.app.feature.setup.domain.model.SetupStatusEvent
import com.agentdesk.app.feature.terminal.domain.PathProvider
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusTailer @Inject constructor(
    private val pathProvider: PathProvider,
    private val parser: StatusLineParser,
    private val dispatchers: DispatcherProvider
) {

    val statusFile: File
        get() = File(pathProvider.setupStateDir, "status.jsonl")

    fun tailEvents(): Flow<SetupStatusEvent> = flow {
        val file = statusFile
        var pointer = 0L

        while (currentCoroutineContext().isActive) {
            if (!file.exists()) {
                delay(300)
                continue
            }

            val length = file.length()
            if (length < pointer) {
                // File was truncated or recreated
                pointer = 0L
            }

            if (length > pointer) {
                RandomAccessFile(file, "r").use { reader ->
                    reader.seek(pointer)
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val parsed = parser.parseLine(line)
                        if (parsed != null) {
                            emit(parsed)
                        }
                        pointer = reader.filePointer
                        line = reader.readLine()
                    }
                }
            }
            delay(300)
        }
    }.flowOn(dispatchers.io)

    fun clear() {
        if (statusFile.exists()) {
            statusFile.delete()
        }
    }
}
