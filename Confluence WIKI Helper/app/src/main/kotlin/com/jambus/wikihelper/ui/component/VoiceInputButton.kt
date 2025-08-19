package com.jambus.wikihelper.ui.component

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import java.util.*

@Composable
fun VoiceInputButton(
    onVoiceResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    
    // Request permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted && isListening) {
            // Start voice recognition
            startVoiceRecognition(context, onVoiceResult) {
                isListening = false
            }
        } else {
            isListening = false
        }
    }
    
    // Voice recognition launcher
    val voiceRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let { text ->
                onVoiceResult(text)
            }
        }
    }
    
    // Animation for listening state
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_button_scale"
    )
    
    IconButton(
        onClick = {
            if (!isListening) {
                isListening = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                isListening = false
            }
        },
        modifier = modifier
            .scale(scale)
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isListening) 
                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                else 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
    ) {
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = if (isListening) "Stop listening" else "Start voice input",
            tint = if (isListening) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
    
    // Show voice input dialog when listening
    if (isListening) {
        VoiceInputDialog(
            onDismiss = { isListening = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceInputDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Listening...",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated mic icon
                val infiniteTransition = rememberInfiniteTransition(label = "mic_animation")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mic_scale"
                )
                
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(scale),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Speak now...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun startVoiceRecognition(
    context: android.content.Context,
    onResult: (String) -> Unit,
    onComplete: () -> Unit
) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        // Note: This would need to be handled by the launcher in the composable
        // For now, we'll just call onComplete
        onComplete()
    } catch (e: Exception) {
        onComplete()
    }
}
