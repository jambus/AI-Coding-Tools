package com.jambus.wikihelper.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jambus.wikihelper.ui.theme.*
import com.jambus.wikihelper.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    onConversationClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadConversations()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "聊天历史",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.createConversation("新对话") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新建对话",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.conversations.isEmpty()) {
                EmptyHistoryState(
                    onCreateNewChat = { viewModel.createConversation("新对话") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.conversations) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation.id) },
                            onDeleteClick = { showDeleteDialog = conversation.id }
                        )
                    }
                }
            }
            
            // 删除确认对话框
            showDeleteDialog?.let { conversationId ->
                DeleteConversationDialog(
                    onConfirm = {
                        viewModel.deleteConversation(conversationId)
                        showDeleteDialog = null
                    },
                    onDismiss = { showDeleteDialog = null }
                )
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ChatViewModel.ConversationUi,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 对话图标
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = PrimaryBlue.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 对话信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conversation.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = conversation.lastMessage ?: "暂无消息",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTime(conversation.updatedAt),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            // 删除按钮
            IconButton(
                onClick = onDeleteClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除对话",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryState(
    onCreateNewChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = PrimaryBlue.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "暂无聊天历史",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "开始您的第一个对话吧",
            fontSize = 16.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCreateNewChat,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("新建对话", color = Color.White)
        }
    }
}

@Composable
fun DeleteConversationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "删除对话",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("确定要删除这个对话吗？此操作无法撤销。")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = WarningRed)
            ) {
                Text("删除", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

private fun formatTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
    }
}
