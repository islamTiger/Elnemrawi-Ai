package com.example.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Chat : Screen("chat", "Chat", Icons.Default.Code)
    object Projects : Screen("projects", "Projects", Icons.Default.Folder)
    object Agent : Screen("agent", "Agent", Icons.Default.Psychology)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    
    // Non-bottom-bar destinations
    object Models : Screen("models", "Models", Icons.Default.Layers)
    object Editor : Screen("editor/{projectId}", "Editor", Icons.Default.Edit) {
        fun createRoute(projectId: String) = "editor/$projectId"
    }
    object GitHub : Screen("github", "GitHub", Icons.Default.Hub)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Chat,
    Screen.Projects,
    Screen.Agent,
    Screen.Settings
)
