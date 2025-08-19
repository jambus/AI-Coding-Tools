package com.jambus.wikihelper.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jambus.wikihelper.ui.theme.*
import com.jambus.wikihelper.ui.viewmodel.ChatViewModel
import com.jambus.wikihelper.ui.component.FileUploadDialog
import com.jambus.wikihelper.ui.component.VoiceInputButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToHistory: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    var showKnowledgeSheet by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf("") }

    // 检查API Key状态（但不强制要求）
    LaunchedEffect(uiState.isApiKeySet) {
        // 可以在这里显示提示，但不强制弹出对话框
        // if (!uiState.isApiKeySet) {
        //     showApiKeyDialog = true
        // }
    }

    // 初始化时加载最新会话或创建新会话
    LaunchedEffect(Unit) {
        if (uiState.currentConversationId == null && uiState.conversations.isNotEmpty()) {
            // 如果有历史会话，加载最新的
            val latestConversation = uiState.conversations.firstOrNull()
            latestConversation?.let { viewModel.loadMessages(it.id) }
        } else if (uiState.currentConversationId == null && uiState.conversations.isEmpty()) {
            // 如果没有会话，创建新会话
            viewModel.createConversation()
        }
    }

    // 自动滚动到底部 - 处理普通消息和流式消息
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scrollState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    // 流式消息时也自动滚动
    LaunchedEffect(uiState.streamingMessage) {
        if (uiState.isStreaming && uiState.streamingMessage.isNotEmpty()) {
            val totalItems = uiState.messages.size + 1 // +1 for streaming message
            scrollState.animateScrollToItem(totalItems - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    onHistoryClick = onNavigateToHistory,
                    onKnowledgeSearchClick = { showKnowledgeSheet = true }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Content area with weight to allow InputBar to show at bottom
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.messages.isEmpty()) {
                        EmptyStateScreen()
                    } else {
                        MessageList(
                            messages = uiState.messages,
                            streamingMessage = uiState.streamingMessage,
                            isStreaming = uiState.isStreaming,
                            scrollState = scrollState,
                            onSourceClick = { source ->
                                selectedSource = source
                                showKnowledgeSheet = true
                            },
                            isLoading = uiState.isLoading
                        )
                    }
                }
                
                // InputBar should always be shown at bottom
                InputBar(
                    onSendMessage = { message, attachments ->
                        // Always allow sending messages, ViewModel will handle API key logic
                        viewModel.sendMessage(message, attachments)
                    }
                )
            }
        }

        // 错误提示
        uiState.error?.let { error ->
            ErrorSnackbar(
                error = error,
                onDismiss = { viewModel.clearError() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // API Key设置对话框
        if (showApiKeyDialog) {
            ApiKeyDialog(
                onDismiss = { showApiKeyDialog = false },
                onConfirm = { apiKey ->
                    viewModel.setApiKey(apiKey)
                    showApiKeyDialog = false
                }
            )
        }

        // 知识来源底部弹窗
        if (showKnowledgeSheet) {
            if (selectedSource.isNotEmpty()) {
                KnowledgeSourceBottomSheet(
                    source = selectedSource,
                    onDismiss = { 
                        showKnowledgeSheet = false
                        selectedSource = ""
                    },
                    onViewFullDocument = { 
                        // TODO: 打开完整文档
                        showKnowledgeSheet = false
                        selectedSource = ""
                    }
                )
            } else {
                KnowledgeManagementScreen(
                    onDismiss = { showKnowledgeSheet = false }
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatViewModel.ChatMessageUi) {
    val backgroundColor = if (message.isUser) {
        Color(0xFFE0E0E0) // 用户消息背景色
    } else {
        Color(0xFF4285F4) // AI消息背景色
    }
    
    val textColor = if (message.isUser) {
        Color.Black
    } else {
        Color.White
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 16.sp
                )
            }
            
            message.references?.let { references ->
                if (references.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        references.split(",").forEach { source ->
                            SourceTag(source.trim())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceTag(source: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF34A853))
            .clickable { /* TODO: 显示来源详情 */ }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "来源",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = PrimaryBlue,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun InputBar(
    onSendMessage: (String, List<String>) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf(listOf<String>()) }
    var showFileUploadDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 语音输入按钮
            VoiceInputButton(
                onVoiceResult = { voiceText ->
                    message = voiceText
                },
                modifier = Modifier.padding(end = 4.dp)
            )
            
            // 文件上传按钮
            IconButton(
                onClick = { showFileUploadDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload file",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = { Text("输入问题...") },
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.Gray
                )
            )
            
            FloatingActionButton(
                onClick = {
                    if (message.isNotBlank()) {
                        onSendMessage(message, attachments)
                        message = ""
                        attachments = emptyList()
                    }
                },
                containerColor = PrimaryBlue,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送",
                    tint = Color.White
                )
            }
        }
        
        // Display selected files
        if (attachments.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attachments) { attachment ->
                    FileChip(
                        fileName = attachment,
                        onRemove = {
                            attachments = attachments.filter { it != attachment }
                        }
                    )
                }
            }
        }
    }
    
    // File upload dialog
    if (showFileUploadDialog) {
        FileUploadDialog(
            onDismiss = { showFileUploadDialog = false },
            onFileSelected = { uri ->
                // Extract filename from URI and add to attachments
                val fileName = uri.lastPathSegment ?: "Unknown file"
                attachments = attachments + fileName
            }
        )
    }
}

@Composable
private fun FileChip(
    fileName: String,
    onRemove: () -> Unit
) {
    InputChip(
        onClick = { },
        label = {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        },
        selected = false,
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove file",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    )
}

// 改进的TopBar组件
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    onHistoryClick: () -> Unit,
    onKnowledgeSearchClick: () -> Unit
) {
    TopAppBar(
        title = { 
            Text(
                "企业知识助手",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryBlue,
            titleContentColor = Color.White
        ),
        actions = {
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "历史记录",
                    tint = Color.White
                )
            }
            IconButton(onClick = onKnowledgeSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "知识库搜索",
                    tint = Color.White
                )
            }
        }
    )
}

// 改进的消息列表组件
@Composable
fun MessageList(
    messages: List<ChatViewModel.ChatMessageUi>,
    streamingMessage: String,
    isStreaming: Boolean,
    scrollState: LazyListState,
    onSourceClick: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = scrollState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = messages,
            key = { message -> message.id } // 使用稳定的key避免重组
        ) { message ->
            // 历史消息使用静态组件，避免重组动画
            StaticMessageBubble(
                message = message,
                onSourceClick = onSourceClick
            )
        }
        
        // 显示正在流式输入的消息
        if (isStreaming && streamingMessage.isNotEmpty()) {
            item(key = "streaming_message") { // 使用固定key避免重复
                // 只有流式消息使用动画组件
                AnimatedMessageBubble(
                    message = ChatViewModel.ChatMessageUi(
                        id = 0, // 临时ID，不会与历史消息冲突
                        text = streamingMessage,
                        isUser = false,
                        timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    ),
                    onSourceClick = onSourceClick,
                    isStreaming = true // 标识这是流式消息
                )
            }
        }
        
        if (isLoading) {
            item {
                TypingIndicator()
            }
        }
    }
}

// 静态消息气泡 - 用于历史消息，避免重组时的动画重播
@Composable
fun StaticMessageBubble(
    message: ChatViewModel.ChatMessageUi,
    onSourceClick: (String) -> Unit
) {
    // 直接显示，无任何动画效果（包括打字机效果）
    EnhancedMessageBubble(
        message = message,
        onSourceClick = onSourceClick,
        useTypewriter = false // 明确禁用打字机效果
    )
}

// 带动画的消息气泡 - 仅用于流式消息
@Composable
fun AnimatedMessageBubble(
    message: ChatViewModel.ChatMessageUi,
    onSourceClick: (String) -> Unit,
    isStreaming: Boolean = false
) {
    // 只有流式消息才有动画效果
    if (isStreaming) {
        var isVisible by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) { // 只在首次显示时触发动画
            isVisible = true
        }
        
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        ) {
            EnhancedMessageBubble(
                message = message,
                onSourceClick = onSourceClick,
                useTypewriter = true // 流式消息使用打字机效果
            )
        }
    } else {
        // 非流式消息直接显示，无动画
        EnhancedMessageBubble(
            message = message,
            onSourceClick = onSourceClick
        )
    }
}

// 增强的消息气泡，支持知识来源
@Composable
fun EnhancedMessageBubble(
    message: ChatViewModel.ChatMessageUi,
    onSourceClick: (String) -> Unit,
    useTypewriter: Boolean = false // 新增参数控制是否使用打字机效果
) {
    val clipboardManager = LocalClipboardManager.current
    val backgroundColor = if (message.isUser) UserMessageBackground else AIMessageBackground
    val textColor = if (message.isUser) TextPrimary else TextOnPrimary
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            // 消息气泡
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    // 长按复制消息
                                    clipboardManager.setText(AnnotatedString(message.text))
                                }
                            )
                        }
                ) {
                    if (message.isUser) {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 16.sp
                        )
                    } else {
                        // AI消息：只有流式消息才使用打字机效果
                        if (useTypewriter) {
                            TypewriterText(
                                text = message.text,
                                color = textColor,
                                fontSize = 16.sp
                            )
                        } else {
                            // 历史消息直接显示完整文本，无动画
                            Text(
                                text = message.text,
                                color = textColor,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            
            // 知识来源标签
            message.references?.let { references ->
                Spacer(modifier = Modifier.height(4.dp))
                KnowledgeSourceChip(
                    references = references,
                    onClick = { onSourceClick(references) }
                )
            }
            
            // 时间戳
            Text(
                text = message.timestamp,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// 打字机效果文本
@Composable
fun TypewriterText(
    text: String,
    color: Color = Color.Black,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    modifier: Modifier = Modifier
) {
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(text) {
        displayedText = ""
        text.forEachIndexed { index, _ ->
            delay(30) // 30ms/字符，符合需求
            displayedText = text.substring(0, index + 1)
        }
    }
    
    Text(
        text = displayedText,
        color = color,
        fontSize = fontSize,
        modifier = modifier
    )
}

// 知识来源标签
@Composable
fun KnowledgeSourceChip(
    references: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AccentGreen,
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "来源",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// 打字指示器
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(AIMessageBackground.copy(alpha = alpha))
                .padding(12.dp)
        ) {
            Row {
                repeat(3) { index ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 200)
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color.White.copy(alpha = dotAlpha),
                                shape = CircleShape
                            )
                    )
                    
                    if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

// 空状态屏幕
@Composable
fun EmptyStateScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = PrimaryBlue.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "欢迎使用企业知识助手",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "您可以问我任何关于公司政策、流程或技术问题",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "💬 在下方输入框开始对话",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "提示：设置Dify API Key可获得AI智能回复",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 错误提示
@Composable
fun ErrorSnackbar(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarningRed)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}

// 知识来源详情底部弹窗
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeSourceBottomSheet(
    source: String,
    onDismiss: () -> Unit,
    onViewFullDocument: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "知识来源",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 模拟知识来源内容
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "文档标题：员工手册 - 第3章",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "部门：人力资源部",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    
                    Text(
                        text = "更新时间：2024年1月15日",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 高亮的原文段落
                    Text(
                        text = "相关内容片段：",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = source,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onViewFullDocument,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("查看完整文档", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// 简化的知识库管理屏幕（后续会创建完整版本）
@Composable
fun KnowledgeManagementScreen(
    onDismiss: () -> Unit
) {
    // 这里先用简单的对话框，后续会实现完整的知识库管理页面
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("知识库搜索") },
        text = { Text("知识库管理功能正在开发中...") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

// API Key 设置对话框
@Composable
fun ApiKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "设置 Dify API Key",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "请输入您的 Dify API Key 以开始使用企业知识助手：",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("输入您的 API Key...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Text(
                    "注意：API Key 将被安全加密存储在设备本地",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (apiKey.isNotBlank()) {
                        onConfirm(apiKey.trim())
                    }
                },
                enabled = apiKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("确定", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
