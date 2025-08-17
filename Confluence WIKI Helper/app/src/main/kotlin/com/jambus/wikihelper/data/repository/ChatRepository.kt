package com.jambus.wikihelper.data.repository

import com.jambus.wikihelper.data.local.dao.ChatDao
import com.jambus.wikihelper.data.local.entity.ChatMessageEntity
import com.jambus.wikihelper.data.local.entity.ConversationEntity
import com.jambus.wikihelper.data.remote.model.DifyRetrieverResource
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatDao: ChatDao
) {

    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return chatDao.getAllConversations()
    }

    fun getChatMessages(conversationId: String?): Flow<List<ChatMessageEntity>> {
        return chatDao.getChatMessages(conversationId)
    }

    suspend fun createConversation(title: String): String {
        val conversationId = UUID.randomUUID().toString()
        val conversation = ConversationEntity(
            id = conversationId,
            title = title,
            lastMessage = null
        )
        chatDao.insertConversation(conversation)
        return conversationId
    }

    suspend fun sendMessage(
        message: String,
        isUser: Boolean,
        conversationId: String?,
        messageId: String? = null,
        references: List<DifyRetrieverResource>? = null
    ): Long {
        val referencesJson = references?.let { 
            // 简化：这里应该使用Gson或Kotlin序列化
            it.joinToString(";") { ref -> ref.document_name }
        }
        
        val chatMessage = ChatMessageEntity(
            message = message,
            isUser = isUser,
            conversationId = conversationId,
            messageId = messageId,
            references = referencesJson
        )
        
        val messageIdDb = chatDao.insertChatMessage(chatMessage)
        
        conversationId?.let {
            chatDao.updateConversationLastMessage(
                conversationId = it,
                lastMessage = message,
                updatedAt = Date()
            )
        }
        
        return messageIdDb
    }

    suspend fun deleteConversation(conversationId: String) {
        chatDao.deleteConversationWithMessages(conversationId)
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteChatMessage(messageId)
    }
}