package com.agentdesk.app.feature.ai.data.http

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseClient @Inject constructor(
    private val httpClientFactory: HttpClientFactory
) {
    private val client: OkHttpClient by lazy { httpClientFactory.createClient() }

    fun streamEvents(request: Request): Flow<SseEvent> = callbackFlow {
        val call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!isClosedForSend) {
                    close(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        close(IOException("HTTP ${resp.code}: $body"))
                        return
                    }

                    val source = resp.body?.byteStream()
                    if (source == null) {
                        close(IOException("Empty response body"))
                        return
                    }

                    try {
                        val reader = BufferedReader(InputStreamReader(source, Charsets.UTF_8))
                        var currentEvent: String? = null
                        val dataBuilder = StringBuilder()
                        var currentId: String? = null

                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val l = line ?: break
                            when {
                                l.startsWith("event:") -> {
                                    currentEvent = l.removePrefix("event:").trim()
                                }
                                l.startsWith("data:") -> {
                                    val dataPart = l.removePrefix("data:").trim()
                                    if (dataBuilder.isNotEmpty()) {
                                        dataBuilder.append("\n")
                                    }
                                    dataBuilder.append(dataPart)
                                }
                                l.startsWith("id:") -> {
                                    currentId = l.removePrefix("id:").trim()
                                }
                                l.isBlank() -> {
                                    // Empty line indicates event boundary in SSE
                                    if (dataBuilder.isNotEmpty()) {
                                        trySend(
                                            SseEvent(
                                                event = currentEvent,
                                                data = dataBuilder.toString(),
                                                id = currentId
                                            )
                                        )
                                        dataBuilder.clear()
                                        currentEvent = null
                                        currentId = null
                                    }
                                }
                            }
                        }

                        // Emit trailing event if stream ended without empty line
                        if (dataBuilder.isNotEmpty()) {
                            trySend(
                                SseEvent(
                                    event = currentEvent,
                                    data = dataBuilder.toString(),
                                    id = currentId
                                )
                            )
                        }

                        close()
                    } catch (e: Exception) {
                        if (e !is CancellationException && !isClosedForSend) {
                            close(e)
                        }
                    }
                }
            }
        })

        awaitClose {
            call.cancel()
        }
    }
}
