package com.jambus.wikihelper.data.remote.model

data class DifyChatRequest(
    val inputs: Map<String, String> = emptyMap(),
    val query: String,
    val user: String,
    val response_mode: String = "streaming",
    val conversation_id: String? = null,
    val files: List<DifyFile>? = null
)

data class DifyFile(
    val type: String, // "image", "document", etc.
    val transfer_method: String, // "remote_url", "local_file"
    val url: String? = null,
    val upload_file_id: String? = null
)

data class DifyChatResponse(
    val answer: String,
    val conversation_id: String? = null,
    val message_id: String? = null,
    val metadata: DifyMetadata? = null,
    val created_at: Long? = null
)

data class DifyMetadata(
    val usage: DifyUsage? = null,
    val retriever_resources: List<DifyRetrieverResource>? = null
)

data class DifyUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class DifyRetrieverResource(
    val dataset_id: String,
    val dataset_name: String,
    val document_name: String,
    val segment_id: String,
    val score: Double,
    val content: String,
    val position: Int
)

data class DifyCompletionRequest(
    val inputs: Map<String, String> = emptyMap(),
    val user: String,
    val response_mode: String = "streaming",
    val files: List<DifyFile>? = null
)

data class DifyErrorResponse(
    val code: String,
    val message: String
)

// 流式响应数据模型
data class DifyStreamResponse(
    val event: String, // "message", "message_end", "error", etc.
    val conversation_id: String? = null,
    val message_id: String? = null,
    val answer: String? = null,
    val created_at: Long? = null,
    val metadata: DifyMetadata? = null
)