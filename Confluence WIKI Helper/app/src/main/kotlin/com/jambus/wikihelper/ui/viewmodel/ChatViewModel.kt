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
        val isLoading: Boolean = false,
        val error: String? = null,
        val currentConversationId: String? = null,
        val isApiKeySet: Boolean = false
    )

    data class ChatMessageUi(
        val id: Long = 0,
        val text: String,
        val isUser: Boolean,
        val timestamp: String,
        val references: String? = null
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        checkApiKey()
        loadConversations()
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
                // Handle conversation updates
            }
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
                            timestamp = msg.timestamp.toString(),
                            references = msg.references
                        )
                    },
                    currentConversationId = conversationId
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
            _uiState.value = _uiState.value.copy(error = "请先设置API密钥")
            return
        }

        viewModelScope.launch {
            // Add user message to local storage
            chatRepository.sendMessage(message, true, _uiState.value.currentConversationId)

            _uiState.value = _uiState.value.copy(isLoading = true)

            // 将文件转换为DifyFile格式（如果有）
            val difyFiles = files.map { fileUrl ->
                com.jambus.wikihelper.data.remote.model.DifyFile(
                    type = "image", // 根据文件类型判断
                    transfer_method = "remote_url",
                    url = fileUrl
                )
            }.takeIf { it.isNotEmpty() }

            try {
                // 使用流式响应
                var fullResponse = ""
                var currentConversationId = _uiState.value.currentConversationId
                var messageId: String? = null
                var retrieverResources: List<com.jambus.wikihelper.data.remote.model.DifyRetrieverResource>? = null

                difyRepository.sendChatMessageStream(
                    apiKey = apiKey,
                    query = message,
                    conversationId = currentConversationId,
                    files = difyFiles
                ).collect { result ->
                    when (result) {
                        is com.jambus.wikihelper.utils.NetworkResult.Success -> {
                            val streamData = result.data
                            
                            when (streamData.event) {
                                "message" -> {
                                    // 累积回答内容
                                    streamData.answer?.let { answer ->
                                        fullResponse += answer
                                        
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
                                                    .format(java.util.Date()),
                                                references = retrieverResources?.joinToString("\n") { resource ->
                                                    "${resource.document_name}: ${resource.content}"
                                                }
                                            ))
                                        }
                                        
                                        _uiState.value = _uiState.value.copy(
                                            messages = currentMessages,
                                            isLoading = true // 仍在加载中
                                        )
                                    }
                                }
                                "message_end" -> {
                                    // 消息结束
                                    currentConversationId = streamData.conversation_id ?: currentConversationId
                                    messageId = streamData.message_id
                                    
                                    // 提取知识来源
                                    streamData.metadata?.retriever_resources?.let { resources ->
                                        retrieverResources = resources
                                    }
                                    
                                    // 保存完整的AI回答到数据库
                                    chatRepository.sendMessage(
                                        message = fullResponse,
                                        isUser = false,
                                        conversationId = currentConversationId,
                                        messageId = messageId,
                                        references = retrieverResources
                                    )
                                    
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        error = null,
                                        currentConversationId = currentConversationId
                                    )
                                }
                                "error" -> {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        error = "AI回答出错，请重试"
                                    )
                                }
                            }
                        }
                        is com.jambus.wikihelper.utils.NetworkResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                        is com.jambus.wikihelper.utils.NetworkResult.Loading -> {
                            // 处理加载状态
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "发送消息失败: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
        }
    }
}