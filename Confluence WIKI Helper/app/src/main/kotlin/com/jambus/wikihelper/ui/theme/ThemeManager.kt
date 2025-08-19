package com.jambus.wikihelper.ui.theme

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    
    private val sharedPrefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    init {
        // 读取保存的主题设置
        val savedTheme = sharedPrefs.getBoolean("dark_theme", false)
        _isDarkTheme.value = savedTheme
        Log.d("ThemeManager", "Initialized with dark theme: $savedTheme")
    }
    
    fun toggleTheme() {
        val newValue = !_isDarkTheme.value
        _isDarkTheme.value = newValue
        
        // 保存主题设置
        sharedPrefs.edit()
            .putBoolean("dark_theme", newValue)
            .apply()
            
        Log.d("ThemeManager", "Theme toggled to: $newValue")
    }
    
    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        sharedPrefs.edit()
            .putBoolean("dark_theme", isDark)
            .apply()
        Log.d("ThemeManager", "Theme set to: $isDark")
    }
}

@Composable
fun ThemeManager.rememberThemeState(): Boolean {
    val isDark by isDarkTheme.collectAsState()
    return isDark
}
