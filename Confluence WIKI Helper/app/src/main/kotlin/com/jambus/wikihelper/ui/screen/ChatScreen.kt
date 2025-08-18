package com.jambus.wikihelper.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    
    Scaffold(
        topBar = {
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
                    IconButton(onClick = { /* TODO: 历史记录 */ }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "历史记录",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { /* TODO: 知识库搜索 */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "知识库搜索",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = false
            ) {
                items(uiState.messages) { message ->
                    ChatMessageItem(message)
                }
                if (uiState.isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }
            
            InputBar(
                onSendMessage = { message, attachments ->
                    viewModel.sendMessage(message)
                }
            )
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
            IconButton(onClick = { /* TODO: 语音输入 */ }) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "语音输入"
                )
            }
            
            IconButton(onClick = { /* TODO: 附件上传 */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "附件上传"
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
    }
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
        items(messages) { message ->
            AnimatedMessageBubble(
                message = message,
                onSourceClick = onSourceClick
            )
        }
        
        if (isLoading) {
            item {
                TypingIndicator()
            }
        }
    }
}

// 带动画的消息气泡
@Composable
fun AnimatedMessageBubble(
    message: ChatViewModel.ChatMessageUi,
    onSourceClick: (String) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(message) {
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
            onSourceClick = onSourceClick
        )
    }
}

// 增强的消息气泡，支持知识来源
@Composable
fun EnhancedMessageBubble(
    message: ChatViewModel.ChatMessageUi,
    onSourceClick: (String) -> Unit
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
                        // AI消息支持打字机效果
                        TypewriterText(
                            text = message.text,
                            color = textColor,
                            fontSize = 16.sp
                        )
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
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "您可以问我任何关于公司政策、流程或技术问题",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = TextSecondary
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
