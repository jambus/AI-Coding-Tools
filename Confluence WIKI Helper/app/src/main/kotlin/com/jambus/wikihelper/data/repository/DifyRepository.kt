package com.jambus.wikihelper.data.repository

import com.jambus.wikihelper.data.remote.api.DifyApiService
import com.jambus.wikihelper.data.remote.model.DifyChatRequest
import com.jambus.wikihelper.data.remote.model.DifyChatResponse
import com.jambus.wikihelper.data.remote.model.DifyCompletionRequest
import com.jambus.wikihelper.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DifyRepository @Inject constructor(
    private val apiService: DifyApiService
) {

    suspend fun sendChatMessage(
        apiKey: String,
        query: String,
        user: String,
        conversationId: String? = null,
        responseMode: String = "streaming"
    ): NetworkResult<DifyChatResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DifyChatRequest(
                    query = query,
                    user = user,
                    conversation_id = conversationId,
                    response_mode = responseMode
                )
                
                val response = apiService.sendChatMessage(
                    authToken = "Bearer $apiKey",
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

    suspend fun sendCompletionMessage(
        apiKey: String,
        user: String,
        inputs: Map<String, String> = emptyMap(),
        responseMode: String = "streaming"
    ): NetworkResult<DifyChatResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DifyCompletionRequest(
                    inputs = inputs,
                    user = user,
                    response_mode = responseMode
                )
                
                val response = apiService.sendCompletionMessage(
                    authToken = "Bearer $apiKey",
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