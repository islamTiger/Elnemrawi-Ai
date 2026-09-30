package com.example.ui.models

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.chat.ChatViewModel
import com.example.core.di.ServiceLocator
import com.example.domain.ai.LocalModelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedModel by viewModel.selectedModel.collectAsState()
    val scrollState = rememberScrollState()

    val modelManager = remember { ServiceLocator.localModelManager }
    val currentConfig by modelManager.currentConfig.collectAsState()
    val modelState by modelManager.modelState.collectAsState()

    val modelOptions = listOf(
        ModelOption("Auto", "Automatic Routing", "Automatically routes request to general, coding, or vision models depending on query intent.", Icons.Default.AutoMode),
        ModelOption("General", "General Assistant", "Llama-3.1-8B-Instruct. Optimized for reasoning, logic explanations, and natural chats.", Icons.Default.Psychology),
        ModelOption("Coding", "Autonomous Coding", "Qwen2.5-Coder-7B or DeepSeek-Coder. Optimized for code mutation, syntax corrections, and project edits.", Icons.Default.Code),
        ModelOption("Vision", "Multimodal Vision", "Gemini-1.5-Pro. Supports screenshot analysis, visual mock interpretations, and APK analyses.", Icons.Default.Visibility),
        ModelOption("Image Generation", "Creative Imagen", "Imagen-3. Premium high-fidelity creative asset generation and vectors.", Icons.Default.Image),
        ModelOption("Image Editing", "Infill Refactoring", "Flux-Instruct. Fine-tune visual vectors, edit image assets, and repaint mocks.", Icons.Default.Brush),
        ModelOption("Local GGUF", "Local Offline GGUF", "Direct on-device GGUF execution. Fully private offline workspace runtime.", Icons.Default.Dns)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Models Registry",
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Advanced inference options. Select an explicit model capability profile or enable automatic orchestration.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .selectableGroup()
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                modelOptions.forEach { option ->
                    val isSelected = selectedModel == option.id
                    val isGgufConfigured = option.id != "Local GGUF" || currentConfig != null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    if (isGgufConfigured) {
                                        viewModel.selectModel(option.id)
                                    }
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.medium
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (isGgufConfigured) {
                                        viewModel.selectModel(option.id)
                                    }
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = option.name,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = option.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (option.id == "Local GGUF") {
                                        Surface(
                                            color = if (isGgufConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = MaterialTheme.shapes.extraSmall
                                        ) {
                                            Text(
                                                text = if (isGgufConfigured) "READY" else "UNCONFIGURED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGgufConfigured) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )

                                if (option.id == "Local GGUF" && currentConfig != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Active file: ${currentConfig?.name ?: "None"} (${modelState})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class ModelOption(
    val id: String,
    val name: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
