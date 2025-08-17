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

    fun sendMessage(message: String) {
        val apiKey = securityManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(error = "请先设置API密钥")
            return
        }

        viewModelScope.launch {
            // Add user message to local storage
            chatRepository.sendMessage(message, true, _uiState.value.currentConversationId)

            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = difyRepository.sendChatMessage(
                apiKey = apiKey,
                query = message,
                user = "user_${System.currentTimeMillis()}",
                conversationId = _uiState.value.currentConversationId
            )) {
                is NetworkResult.Success -> {
                    val response = result.data
                    chatRepository.sendMessage(
                        message = response.answer,
                        isUser = false,
                        conversationId = _uiState.value.currentConversationId,
                        messageId = response.message_id,
                        references = response.metadata?.retriever_resources
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
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