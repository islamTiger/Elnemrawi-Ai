package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.navigation.Screen
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.projects.ProjectsScreen
import com.example.ui.projects.ProjectsViewModel
import com.example.ui.editor.CodeEditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.github.GitHubScreen
import com.example.ui.github.GitHubViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.models.ModelsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.core.di.ServiceLocator.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppWorkspaceShell()
            }
        }
    }
}

@Composable
fun AppWorkspaceShell() {
    val navController = rememberNavController()
    
    // Instantiate separate ViewModels scoped correctly
    val chatViewModel: ChatViewModel = viewModel()
    val projectsViewModel: ProjectsViewModel = viewModel()
    val editorViewModel: EditorViewModel = viewModel()
    val gitHubViewModel: GitHubViewModel = viewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Chat is the primary and default screen
            composable(Screen.Chat.route) {
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToModels = { navController.navigate(Screen.Models.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToGitHub = { navController.navigate(Screen.GitHub.route) }
                )
            }
            
            composable(Screen.Models.route) {
                ModelsScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Projects.route) {
                ProjectsScreen(
                    viewModel = projectsViewModel,
                    onNavigateToEditor = { projId -> navController.navigate(Screen.Editor.createRoute(projId)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                CodeEditorScreen(
                    viewModel = editorViewModel,
                    projectId = projectId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.GitHub.route) {
                GitHubScreen(
                    viewModel = gitHubViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
