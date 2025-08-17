package com.jambus.wikihelper.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class DifyChatRequest(
    val inputs: Map<String, String> = emptyMap(),
    val query: String,
    val user: String,
    val response_mode: String = "streaming",
    val conversation_id: String? = null
)

@Serializable
data class DifyChatResponse(
    val answer: String,
    val conversation_id: String? = null,
    val message_id: String? = null,
    val metadata: DifyMetadata? = null
)

@Serializable
data class DifyMetadata(
    val usage: DifyUsage? = null,
    val retriever_resources: List<DifyRetrieverResource>? = null
)

@Serializable
data class DifyUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

@Serializable
data class DifyRetrieverResource(
    val dataset_id: String,
    val dataset_name: String,
    val document_name: String,
    val segment_id: String,
    val score: Double,
    val content: String,
    val position: Int
)

@Serializable
data class DifyCompletionRequest(
    val inputs: Map<String, String> = emptyMap(),
    val user: String,
    val response_mode: String = "streaming"
)

@Serializable
data class DifyErrorResponse(
    val code: String,
    val message: String
)