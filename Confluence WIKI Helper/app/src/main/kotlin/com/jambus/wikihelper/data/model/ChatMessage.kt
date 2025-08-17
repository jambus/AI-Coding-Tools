package com.jambus.wikihelper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val isUser: Boolean,
    val timestamp: Date = Date(),
    val sources: List<String>? = null,
    val conversationId: String? = null,
    val attachments: List<String>? = null
)

@Entity(tableName = "knowledge_documents")
data class KnowledgeDocument(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val department: String,
    val type: String,
    val lastUpdated: String,
    val rating: Int,
    val icon: String? = null
)

@Entity(tableName = "dify_conversations")
data class DifyConversation(
    @PrimaryKey
    val id: String,
    val title: String,
    val lastMessage: String,
    val lastUpdated: Date = Date(),
    val unreadCount: Int = 0
)

// API请求数据模型
@kotlinx.serialization.Serializable
data class DifyChatRequest(
    val query: String,
    val user: String,
    val response_mode: String = "streaming",
    val conversation_id: String? = null,
    val files: List<String>? = null
)

@kotlinx.serialization.Serializable
data class DifyChatResponse(
    val answer: String,
    val conversation_id: String? = null,
    val sources: List<KnowledgeSource>? = null
)

@kotlinx.serialization.Serializable
data class KnowledgeSource(
    val title: String,
    val content: String,
    val url: String? = null,
    val score: Double? = null
)

@kotlinx.serialization.Serializable
data class DifyCompletionRequest(
    val inputs: Map<String, String>,
    val user: String,
    val response_mode: String = "blocking"
)

@kotlinx.serialization.Serializable
data class DifyCompletionResponse(
    val answer: String,
    val sources: List<KnowledgeSource>? = null
)

// 多模态输入支持
sealed class InputType {
    data class Text(val content: String) : InputType()
    data class Image(val filePath: String, val ocrText: String? = null) : InputType()
    data class File(val filePath: String, val summary: String? = null) : InputType()
    data class Voice(val audioPath: String, val transcript: String) : InputType()
}

data class Attachment(
    val type: String, // "image", "pdf", "word", "audio"
    val path: String,
    val name: String,
    val size: Long,
    val preview: String? = null
)