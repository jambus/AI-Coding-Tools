package com.jambus.wikihelper.data.repository

import com.jambus.wikihelper.data.remote.api.DifyApiService
import com.jambus.wikihelper.data.remote.model.*
import com.jambus.wikihelper.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("DifyRepository", "API Error - Code: ${response.code()}, Message: ${response.message()}")
                    android.util.Log.e("DifyRepository", "Error Body: $errorBody")
                    
                    val errorMessage = try {
                        val errorJson = JSONObject(errorBody ?: "")
                        errorJson.optString("message", "Unknown error")
                    } catch (e: Exception) {
                        "API Error: ${response.code()} - ${response.message()}\nURL: ${response.raw().request.url}"
                    }
                    NetworkResult.Error(errorMessage)
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
    ): Flow<NetworkResult<DifyStreamResponse>> = flow {
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
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    line?.let { currentLine ->
                        if (currentLine.startsWith("data: ")) {
                            val jsonData = currentLine.substring(6).trim()
                            if (jsonData != "[DONE]" && jsonData.isNotEmpty()) {
                                try {
                                    val streamData = parseStreamResponse(jsonData)
                                    emit(NetworkResult.Success(streamData))
                                } catch (e: Exception) {
                                    emit(NetworkResult.Error("Stream parsing error: ${e.message}"))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error("Stream error: ${e.message}"))
        }
    }

    private fun parseStreamResponse(jsonData: String): DifyStreamResponse {
        val json = JSONObject(jsonData)
        return DifyStreamResponse(
            event = json.optString("event", ""),
            conversation_id = json.optString("conversation_id"),
            message_id = json.optString("message_id"),
            answer = json.optString("answer"),
            created_at = json.optLong("created_at"),
            metadata = parseMetadata(json.optJSONObject("metadata"))
        )
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
        
        val retrieverResources = metadataJson.optJSONArray("retriever_resources")?.let { array ->
            mutableListOf<DifyRetrieverResource>().apply {
                for (i in 0 until array.length()) {
                    val resourceJson = array.getJSONObject(i)
                    add(DifyRetrieverResource(
                        dataset_id = resourceJson.optString("dataset_id"),
                        dataset_name = resourceJson.optString("dataset_name"),
                        document_name = resourceJson.optString("document_name"),
                        segment_id = resourceJson.optString("segment_id"),
                        score = resourceJson.optDouble("score"),
                        content = resourceJson.optString("content"),
                        position = resourceJson.optInt("position")
                    ))
                }
            }
        }
        
        return DifyMetadata(usage = usage, retriever_resources = retrieverResources)
    }

    suspend fun sendCompletionMessage(
        apiKey: String,
        user: String = DEFAULT_USER_ID,
        inputs: Map<String, String> = emptyMap(),
        responseMode: String = "blocking",
        files: List<DifyFile>? = null
    ): NetworkResult<DifyChatResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DifyCompletionRequest(
                    inputs = inputs,
                    user = user,
                    response_mode = responseMode,
                    files = files
                )
                
                val response = apiService.sendCompletionMessage(
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
}