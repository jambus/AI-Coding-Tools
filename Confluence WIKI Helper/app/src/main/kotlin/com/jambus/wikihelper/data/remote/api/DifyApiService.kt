package com.jambus.wikihelper.data.remote.api

import com.jambus.wikihelper.data.remote.model.DifyChatRequest
import com.jambus.wikihelper.data.remote.model.DifyChatResponse
import com.jambus.wikihelper.data.remote.model.DifyCompletionRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface DifyApiService {
    
    @Headers("Content-Type: application/json")
    @POST("v1/chat-messages")
    suspend fun sendChatMessage(
        @Body request: DifyChatRequest
    ): Response<DifyChatResponse>

    @Headers("Content-Type: application/json")
    @POST("v1/completion-messages")
    suspend fun sendCompletionMessage(
        @Body request: DifyCompletionRequest
    ): Response<DifyChatResponse>

    @Headers("Content-Type: application/json")
    @Streaming
    @POST("v1/chat-messages")
    suspend fun sendChatMessageStream(
        @Body request: DifyChatRequest
    ): ResponseBody
}