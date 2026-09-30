package com.example.ui.github

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubScreen(
    viewModel: GitHubViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val accountName by viewModel.accountName.collectAsState()
    val remoteRepositories by viewModel.remoteRepositories.collectAsState()
    val tokenInput by viewModel.tokenInput.collectAsState()
    val cloneUrlInput by viewModel.cloneUrlInput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GitHub Client Integration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back to chat")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isWideScreen) 32.dp else 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Securely synchronize your remote code repos. Clone remote directories into local projects or commit agent changes back directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isConnected) {
                    // Not Connected Auth view
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, "Unconnected", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GitHub Connection Unconfigured",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Text(
                                text = "To authorize Nemrawy Code AI to access your repositories, generate a Personal Access Token (PAT) with 'repo' scopes in Developer Settings on GitHub, and paste it below. (For testing, enter a key starting with 'ghp_')",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { viewModel.updateTokenInput(it) },
                                label = { Text("Personal Access Token (PAT)") },
                                placeholder = { Text("ghp_...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("github_token_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Button(
                                onClick = { viewModel.connectGitHub() },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("github_connect_button"),
                                shape = MaterialTheme.shapes.small
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.Login, "Connect")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect GitHub PAT", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // Connected Dashboard View
                    if (isWideScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Pane: Profile and Git actions
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ProfileCard(accountName = accountName ?: "user", onDisconnect = { viewModel.disconnectGitHub() })
                                CloneManualCard(
                                    url = cloneUrlInput,
                                    onUrlChange = { viewModel.updateCloneUrlInput(it) },
                                    onClone = { viewModel.cloneRepo(it) },
                                    isLoading = isLoading
                                )
                            }

                            // Right Pane: Remote repos list
                            Column(
                                modifier = Modifier
                                    .weight(1.8f)
                                    .fillMaxHeight()
                            ) {
                                Text(
                                    text = "AVAILABLE REMOTE REPOSITORIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                RemoteReposList(repos = remoteRepositories, onClone = { viewModel.cloneRepo(it) })
                            }
                        }
                    } else {
                        // Mobile Portrait Layout
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            ProfileCard(accountName = accountName ?: "user", onDisconnect = { viewModel.disconnectGitHub() })
                            Spacer(modifier = Modifier.height(16.dp))
                            CloneManualCard(
                                url = cloneUrlInput,
                                onUrlChange = { viewModel.updateCloneUrlInput(it) },
                                onClone = { viewModel.cloneRepo(it) },
                                isLoading = isLoading
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "REMOTE REPOSITORIES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                RemoteReposList(repos = remoteRepositories, onClone = { viewModel.cloneRepo(it) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCard(accountName: String, onDisconnect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountCircle, "Account", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Connected Account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(text = "@$accountName", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
            
            TextButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
fun CloneManualCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onClone: (String) -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "CLONE REPOSITORY BY HTTPS URL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = { Text("https://github.com/user/repo") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("github_clone_url_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Button(
                onClick = { onClone(url) },
                enabled = !isLoading && url.isNotBlank(),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, "Clone")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clone Remote Repo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RemoteReposList(repos: List<Project>, onClone: (String) -> Unit) {
    if (repos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text("No remote repositories visible.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(repos) { repo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = repo.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(text = repo.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { repo.repositoryUrl?.let { onClone(it) } },
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Clone", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
