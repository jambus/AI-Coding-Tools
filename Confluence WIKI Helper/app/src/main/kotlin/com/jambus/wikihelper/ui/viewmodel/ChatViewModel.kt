package com.jambus.wikihelper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jambus.wikihelper.data.repository.ChatRepository
import com.jambus.wikihelper.data.repository.DifyRepository
import com.jambus.wikihelper.data.security.SecurityManager
import com.jambus.wikihelper.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val difyRepository: DifyRepository,
    private val chatRepository: ChatRepository,
    private val securityManager: SecurityManager
) : ViewModel() {

    data class ChatUiState(
        val messages: List<ChatMessageUi> = emptyList(),
        val conversations: List<ConversationUi> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val currentConversationId: String? = null,  // 本地数据库conversation ID
        val difyConversationId: String? = null,     // Dify API conversation ID
        val isApiKeySet: Boolean = false
    )

    data class ChatMessageUi(
        val id: Long = 0,
        val text: String,
        val isUser: Boolean,
        val timestamp: String,
        val references: String? = null
    )

    data class ConversationUi(
        val id: String,
        val title: String,
        val lastMessage: String?,
        val updatedAt: java.util.Date
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        checkApiKey()
        loadConversations()
        // 在加载完会话后，如果没有当前会话则加载最新的或创建新的
        viewModelScope.launch {
            chatRepository.getAllConversations().collect { conversations ->
                if (_uiState.value.currentConversationId == null && conversations.isNotEmpty()) {
                    // 加载最新的会话
                    loadMessages(conversations.first().id)
                }
            }
        }
    }

    private fun checkApiKey() {
        val hasApiKey = securityManager.hasApiKey()
        _uiState.value = _uiState.value.copy(isApiKeySet = hasApiKey)
    }

    fun setApiKey(apiKey: String) {
        securityManager.storeApiKey(apiKey)
        _uiState.value = _uiState.value.copy(isApiKeySet = true)
    }

    fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getAllConversations().collectLatest { conversations ->
                _uiState.value = _uiState.value.copy(
                    conversations = conversations.map { conv ->
                        ConversationUi(
                            id = conv.id,
                            title = conv.title,
                            lastMessage = conv.lastMessage,
                            updatedAt = conv.updatedAt
                        )
                    }
                )
            }
        }
    }

    fun createConversation(title: String = "新对话") {
        viewModelScope.launch {
            val conversationId = chatRepository.createConversation(title)
            loadMessages(conversationId)
        }
    }

    fun sendMessage(message: String, files: List<String> = emptyList()) {
        val apiKey = securityManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            // For testing purposes, add a mock response
            addMockResponse(message)
            return
        }

        viewModelScope.launch {
            // Ensure we have a conversation to save messages to
            var conversationId = _uiState.value.currentConversationId
            if (conversationId == null) {
                conversationId = chatRepository.createConversation("新对话")
                _uiState.value = _uiState.value.copy(currentConversationId = conversationId)
            }
            
            // Add user message to local storage
            chatRepository.sendMessage(message, true, conversationId)

            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // 使用流式响应
                var fullResponse = ""

                difyRepository.streamChatMessage(
                    message = message,
                    conversationId = _uiState.value.difyConversationId,  // 使用Dify的conversation ID
                    onConversationIdReceived = { difyConversationId ->
                        // 如果是新对话，保存Dify返回的conversation_id
                        if (_uiState.value.difyConversationId == null) {
                            _uiState.value = _uiState.value.copy(difyConversationId = difyConversationId)
                            android.util.Log.d("ChatViewModel", "Saved Dify conversation_id: $difyConversationId")
                        }
                    }
                ).collect { streamResponse ->
                    // 直接处理字符串响应
                    fullResponse += streamResponse
                    android.util.Log.d("ChatViewModel", "Stream chunk: '$streamResponse', Full response so far: '$fullResponse'")
                    
                    // 实时更新UI显示（支持打字机效果）
                    val currentMessages = _uiState.value.messages.toMutableList()
                    val aiMessageIndex = currentMessages.indexOfLast { !it.isUser }
                    
                    if (aiMessageIndex >= 0) {
                        // 更新现有AI消息
                        currentMessages[aiMessageIndex] = currentMessages[aiMessageIndex].copy(
                            text = fullResponse
                        )
                    } else {
                        // 添加新的AI消息
                        currentMessages.add(ChatMessageUi(
                            text = fullResponse,
                            isUser = false,
                            timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date())
                        ))
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        messages = currentMessages,
                        isLoading = true // 仍在加载中
                    )
                }
                
                // 流结束后保存完整的AI回答到数据库
                chatRepository.sendMessage(
                    message = fullResponse,
                    isUser = false,
                    conversationId = conversationId
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "发送消息失败: ${e.message}"
                )
                android.util.Log.e("ChatViewModel", "Send message error", e)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val conversationId = chatRepository.createConversation("新对话")
            _uiState.value = _uiState.value.copy(
                currentConversationId = conversationId,
                difyConversationId = null, // 重置Dify conversation ID
                messages = emptyList()
            )
        }
    }

    fun loadMessages(conversationId: String?) {
        viewModelScope.launch {
            chatRepository.getChatMessages(conversationId).collectLatest { messages ->
                _uiState.value = _uiState.value.copy(
                    messages = messages.map { msg ->
                        ChatMessageUi(
                            id = msg.id,
                            text = msg.message,
                            isUser = msg.isUser,
                            timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                .format(msg.timestamp),
                            references = msg.references
                        )
                    },
                    currentConversationId = conversationId
                )
            }
        }
    }

    // Mock response for testing when no API key is set
    private fun addMockResponse(userMessage: String) {
        viewModelScope.launch {
            // Ensure we have a conversation to save messages to
            var conversationId = _uiState.value.currentConversationId
            if (conversationId == null) {
                conversationId = chatRepository.createConversation("新对话")
                _uiState.value = _uiState.value.copy(currentConversationId = conversationId)
            }
            
            // Save user message to database
            chatRepository.sendMessage(
                message = userMessage,
                isUser = true,
                conversationId = conversationId
            )
            
            // Generate mock AI response
            val mockResponse = when {
                userMessage.contains("你好") || userMessage.contains("hello") -> 
                    "您好！我是企业知识助手。我可以帮助您解答关于公司政策、流程和技术问题。请注意：当前为演示模式，请设置Dify API Key以获得完整功能。"
                userMessage.contains("测试") || userMessage.contains("test") ->
                    "测试功能正常！聊天界面工作正常。要获得AI回复，请在设置中配置您的Dify API Key。"
                else ->
                    "我收到了您的消息：\"$userMessage\"。为了提供智能回复，请在设置页面中配置您的Dify API Key。"
            }
            
            // Save AI response to database
            chatRepository.sendMessage(
                message = mockResponse,
                isUser = false,
                conversationId = conversationId,
                references = emptyList()
            )
            
            // Messages will be automatically updated through the Flow from database
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
            if (_uiState.value.currentConversationId == conversationId) {
                _uiState.value = _uiState.value.copy(
                    currentConversationId = null,
                    difyConversationId = null,
                    messages = emptyList()
                )
            }
        }
    }
}