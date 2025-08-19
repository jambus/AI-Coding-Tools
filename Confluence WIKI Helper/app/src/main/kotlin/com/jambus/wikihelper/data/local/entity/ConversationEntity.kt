package com.jambus.wikihelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val lastMessage: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
