package com.jambus.wikihelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val message: String,
    val isUser: Boolean,
    val timestamp: Date = Date(),
    val conversationId: String? = null,
    val messageId: String? = null,
    val references: String? = null // JSON string for references
)