package com.jambus.wikihelper.data.remote.api

import com.jambus.wikihelper.data.remote.model.DifyChatRequest
import com.jambus.wikihelper.data.remote.model.DifyChatResponse
import com.jambus.wikihelper.data.remote.model.DifyCompletionRequest
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface DifyApiService {
    
    @Headers("Content-Type: application/json")
    @POST("chat-messages")
    suspend fun sendChatMessage(
        @Header("Authorization") authToken: String,
        @Body request: DifyChatRequest
    ): Response<DifyChatResponse>

    @Headers("Content-Type: application/json")
    @POST("completion-messages")
    suspend fun sendCompletionMessage(
        @Header("Authorization") authToken: String,
        @Body request: DifyCompletionRequest
    ): Response<DifyChatResponse>

    @Headers("Content-Type: application/json")
    @POST("chat-messages")
    suspend fun sendChatMessageStream(
        @Header("Authorization") authToken: String,
        @Body request: DifyChatRequest
    ): ResponseBody
}