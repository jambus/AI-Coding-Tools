package com.jambus.wikihelper.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jambus.wikihelper.ui.theme.PrimaryBlue
import com.jambus.wikihelper.ui.viewmodel.ChatViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable

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
