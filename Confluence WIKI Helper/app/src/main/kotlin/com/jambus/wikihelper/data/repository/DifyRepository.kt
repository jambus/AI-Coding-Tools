package com.jambus.wikihelper.data.repository

import com.jambus.wikihelper.data.remote.api.DifyApiService
import com.jambus.wikihelper.data.remote.model.*
import com.jambus.wikihelper.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.BufferedReader
import javax.inject.Inject

class DifyRepository @Inject constructor(
    private val apiService: DifyApiService
) {

    companion object {
        private const val DEFAULT_USER_ID = "wiki_chat_app" // 根据requirements.md设置默认用户ID
    }

    suspend fun sendChatMessage(
        apiKey: String,
        query: String,
        user: String = DEFAULT_USER_ID,
        conversationId: String? = null,
        responseMode: String = "blocking",
        files: List<DifyFile>? = null
    ): NetworkResult<DifyChatResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DifyChatRequest(
                    query = query,
                    user = user,
                    conversation_id = conversationId,
                    response_mode = responseMode,
                    files = files
                )
                
                val response = apiService.sendChatMessage(
                    request = request
                )
                
                if (response.isSuccessful && response.body() != null) {
                    NetworkResult.Success(response.body()!!)
                } else {
                    NetworkResult.Error("API Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                NetworkResult.Error("Network Error: ${e.message}")
            }
        }
    }

    suspend fun sendChatMessageStream(
        apiKey: String,
        query: String,
        user: String = DEFAULT_USER_ID,
        conversationId: String? = null,
        files: List<DifyFile>? = null
    ): Flow<NetworkResult<DifyStreamResponse>> = flow<NetworkResult<DifyStreamResponse>> {
        try {
            val request = DifyChatRequest(
                query = query,
                user = user,
                conversation_id = conversationId,
                response_mode = "streaming",
                files = files
            )
            
            val responseBody = apiService.sendChatMessageStream(
                request = request
            )
            
            responseBody.source().use { source ->
                val reader = BufferedReader(source.inputStream().reader())
                
                android.util.Log.d("DifyStream", "Starting stream reading...")
                
                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { currentLine ->
                            android.util.Log.d("DifyStream", "Received line: '$currentLine'")
                            
                            if (currentLine.startsWith("data: ")) {
                                val jsonData = currentLine.substring(6).trim()
                                android.util.Log.d("DifyStream", "Extracted JSON data: '$jsonData'")
                                
                                when {
                                    jsonData == "[DONE]" -> {
                                        android.util.Log.d("DifyStream", "Stream completed with [DONE]")
                                        return@use
                                    }
                                    jsonData.isNotEmpty() -> {
                                        try {
                                            val streamData = parseStreamResponse(jsonData)
                                            android.util.Log.d("DifyStream", "Parsed stream data: event=${streamData.event}, answer='${streamData.answer}'")
                                            
                                            // 检查是否是流结束事件
                                            if (streamData.event == "message_end" || streamData.event == "agent_message_end") {
                                                android.util.Log.d("DifyStream", "Stream ended with event: ${streamData.event}")
                                                emit(NetworkResult.Success(streamData))
                                            return@flow
                                        } else {
                                            emit(NetworkResult.Success(streamData))
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("DifyStream", "Stream parsing error", e)
                                        emit(NetworkResult.Error("Stream parsing error: ${e.message}"))
                                    }
                                }
                                else -> {
                                    android.util.Log.d("DifyStream", "Skipping empty JSON data")
                                }
                            }
                        } else if (currentLine.trim().isEmpty()) {
                            android.util.Log.d("DifyStream", "Received empty line (heartbeat)")
                        } else {
                            android.util.Log.d("DifyStream", "Received non-data line: '$currentLine'")
                        }
                    }
                }
            } catch (e: java.io.IOException) {
                android.util.Log.e("DifyStream", "IOException during stream reading", e)
                emit(NetworkResult.Error("Stream connection error: ${e.message}"))
            } catch (e: Exception) {
                android.util.Log.e("DifyStream", "Unexpected error during stream reading", e)
                emit(NetworkResult.Error("Stream error: ${e.message}"))
            }
            
            android.util.Log.d("DifyStream", "Stream reading completed")
        }
    } catch (e: Exception) {
        emit(NetworkResult.Error("Stream error: ${e.message}"))
    }
}.flowOn(Dispatchers.IO)

    suspend fun streamChatMessage(
        message: String,
        conversationId: String?,
        onConversationIdReceived: ((String) -> Unit)? = null
    ): Flow<String> = flow<String> {
        try {
            val validConversationId = if (conversationId.isNullOrBlank()) {
                null
            } else {
                conversationId
            }
            
            android.util.Log.d("DifyRepository", "Sending stream chat message with conversation_id: $validConversationId")
            
            val request = DifyChatRequest(
                query = message,
                user = "default_user",
                conversation_id = validConversationId,
                response_mode = "streaming",
                files = null
            )
            
            val responseBody = apiService.sendChatMessageStream(
                request = request
            )
            
            responseBody.source().use { source ->
                val reader = BufferedReader(source.inputStream().reader())
                
                android.util.Log.d("DifyRepository", "Starting stream reading...")
                
                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let { currentLine ->
                            android.util.Log.d("DifyRepository", "Raw line: '$currentLine'")
                            
                            if (currentLine.startsWith("data: ")) {
                                val jsonData = currentLine.substring(6).trim()
                                android.util.Log.d("DifyRepository", "Raw stream data: $jsonData")
                                if (jsonData != "[DONE]" && jsonData.isNotEmpty()) {
                                    try {
                                        val streamData = parseStreamResponse(jsonData)
                                        android.util.Log.d("DifyRepository", "Parsed event: ${streamData.event}, answer: ${streamData.answer}")
                                        
                                        when (streamData.event) {
                                            "message", "agent_message" -> {
                                                streamData.answer?.let { answer ->
                                                    android.util.Log.d("DifyRepository", "Emitting answer chunk: '$answer'")
                                                    emit(answer)
                                                }
                                            }
                                            "message_delta" -> {
                                                streamData.answer?.let { answer ->
                                                    android.util.Log.d("DifyRepository", "Emitting delta chunk: '$answer'")
                                                    emit(answer)
                                                }
                                            }
                                            "message_end" -> {
                                                android.util.Log.d("DifyRepository", "Stream ended normally")
                                                streamData.conversation_id?.let { cid ->
                                                    android.util.Log.d("DifyRepository", "Received Dify conversation_id: $cid")
                                                    onConversationIdReceived?.invoke(cid)
                                                }
                                                return@use
                                            }
                                            "error" -> {
                                                android.util.Log.e("DifyRepository", "Dify API error in stream: ${streamData.answer}")
                                                throw Exception("Dify API error: ${streamData.answer}")
                                            }
                                            else -> {
                                                android.util.Log.d("DifyRepository", "Unknown event type: ${streamData.event}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("DifyRepository", "Stream parsing error: ${e.message}")
                                        android.util.Log.e("DifyRepository", "Failed to parse: $jsonData")
                                        throw e
                                    }
                                } else if (jsonData == "[DONE]") {
                                    android.util.Log.d("DifyRepository", "Stream completed with [DONE]")
                                    return@use
                                }
                            }
                        }
                    }
                    android.util.Log.d("DifyRepository", "Stream reading completed normally")
                } catch (e: java.io.IOException) {
                    android.util.Log.e("DifyRepository", "IOException during stream reading: ${e.javaClass.simpleName}: ${e.message}")
                    android.util.Log.e("DifyRepository", "IOException stack trace: ${e.stackTraceToString()}")
                    throw Exception("Stream connection error: ${e.message}")
                } catch (e: Exception) {
                    android.util.Log.e("DifyRepository", "Error reading stream: ${e.javaClass.simpleName}: ${e.message}")
                    android.util.Log.e("DifyRepository", "Exception stack trace: ${e.stackTraceToString()}")
                    throw e
                }
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            val exceptionType = e.javaClass.simpleName
            android.util.Log.e("DifyRepository", "Stream error - Type: $exceptionType, Message: '$errorMessage'")
            android.util.Log.e("DifyRepository", "Stream error stack trace: ${e.stackTraceToString()}")
        }
    }.flowOn(Dispatchers.IO)

    private fun parseStreamResponse(jsonData: String): DifyStreamResponse {
        try {
            val json = JSONObject(jsonData)
            android.util.Log.d("DifyStreamParse", "Parsing JSON: $jsonData")
            
            // 尝试从不同字段获取答案内容
            val answer = when {
                json.has("answer") -> {
                    val answerValue = json.optString("answer")
                    android.util.Log.d("DifyStreamParse", "Found answer field: '$answerValue'")
                    answerValue
                }
                json.has("delta") -> {
                    val deltaValue = json.optString("delta")
                    android.util.Log.d("DifyStreamParse", "Found delta field: '$deltaValue'")
                    deltaValue
                }
                json.has("content") -> {
                    val contentValue = json.optString("content")
                    android.util.Log.d("DifyStreamParse", "Found content field: '$contentValue'")
                    contentValue
                }
                else -> {
                    android.util.Log.d("DifyStreamParse", "No answer/delta/content field found")
                    null
                }
            }
            
            val event = json.optString("event", "")
            val conversationId = json.optString("conversation_id")
            val messageId = json.optString("message_id")
            
            android.util.Log.d("DifyStreamParse", "Parsed - event: '$event', conversation_id: '$conversationId', message_id: '$messageId', answer: '$answer'")
            
            return DifyStreamResponse(
                event = event,
                conversation_id = conversationId,
                message_id = messageId,
                answer = answer,
                created_at = json.optLong("created_at"),
                metadata = parseMetadata(json.optJSONObject("metadata"))
            )
        } catch (e: Exception) {
            android.util.Log.e("DifyStreamParse", "Failed to parse stream response: $jsonData", e)
            throw e
        }
    }

    private fun parseMetadata(metadataJson: JSONObject?): DifyMetadata? {
        if (metadataJson == null) return null
        
        val usage = metadataJson.optJSONObject("usage")?.let { usageJson ->
            DifyUsage(
                prompt_tokens = usageJson.optInt("prompt_tokens"),
                completion_tokens = usageJson.optInt("completion_tokens"),
                total_tokens = usageJson.optInt("total_tokens")
            )
        }
        
        return DifyMetadata(
            usage = usage,
            retriever_resources = emptyList() // 简化处理，根据需要可以添加更多解析
        )
    }
}
