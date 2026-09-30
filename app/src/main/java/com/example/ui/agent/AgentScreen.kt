package com.example.ui.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val currentTask by viewModel.currentTask.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()

    var taskPromptInput by remember { mutableStateOf("") }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isWideScreen) 32.dp else 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Autonomous Agent Workspace",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Assign autonomous coding tasks. The agent will formulate a plan, read project context, modify files, verify builds, and commit fixes step-by-step.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (currentTask == null) {
                // Initial Task input State
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Project Context Selection Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TARGET TARGET PROJECT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clickable { projectDropdownExpanded = true }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedProject?.name ?: "No project chosen",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Icon(Icons.Default.ArrowDropDown, "Select")
                                    }

                                    DropdownMenu(
                                        expanded = projectDropdownExpanded,
                                        onDismissRequest = { projectDropdownExpanded = false }
                                    ) {
                                        projects.forEach { proj ->
                                            DropdownMenuItem(
                                                text = { Text(proj.name) },
                                                onClick = {
                                                    viewModel.selectProject(proj)
                                                    projectDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Icon(Icons.Default.FolderOpen, "Project", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    // Task Instruction Field
                    OutlinedTextField(
                        value = taskPromptInput,
                        onValueChange = { taskPromptInput = it },
                        label = { Text("Task Instructions") },
                        placeholder = { Text("e.g. Analyze the project and fix the categories screen.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("agent_task_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Button(
                        onClick = {
                            viewModel.submitTask(taskPromptInput)
                            taskPromptInput = ""
                        },
                        enabled = selectedProject != null && taskPromptInput.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_agent_task_button"),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.PlayArrow, "Start")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Launch Coding Agent", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Running Task State Layout
                val task = currentTask!!
                if (isWideScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Plan Pipeline List
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "AGENT ENGINE PIPELINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(task.steps) { step ->
                                    AgentStepRow(step = step)
                                }
                            }

                            // Control Bar
                            AgentControlBar(
                                task = task,
                                isExecuting = isExecuting,
                                onApprove = { viewModel.approveStep() },
                                onCancel = { viewModel.cancelTask() }
                            )
                        }

                        // Right Column: Terminal Logs & File Changes
                        Column(
                            modifier = Modifier
                                .weight(1.8f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "TERMINAL & DIAGNOSTIC PRINTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            AgentTerminalOutput(
                                logs = task.logs,
                                modifier = Modifier.weight(1.5f)
                            )

                            if (task.filesChanged.isNotEmpty()) {
                                AgentChangedFiles(
                                    files = task.filesChanged,
                                    modifier = Modifier.weight(0.5f)
                                )
                            }
                        }
                    }
                } else {
                    // Mobile Portrait Screen layout
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "PIPELINE PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        task.steps.forEach { step ->
                            AgentStepRow(step = step)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "TERMINAL OUTPUT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        AgentTerminalOutput(
                            logs = task.logs,
                            modifier = Modifier.height(240.dp)
                        )

                        if (task.filesChanged.isNotEmpty()) {
                            AgentChangedFiles(
                                files = task.filesChanged,
                                modifier = Modifier.height(100.dp)
                            )
                        }

                        // Bottom Controls
                        AgentControlBar(
                            task = task,
                            isExecuting = isExecuting,
                            onApprove = { viewModel.approveStep() },
                            onCancel = { viewModel.cancelTask() }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AgentStepRow(step: AgentStep) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (step.status) {
                    StepStatus.RUNNING -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = MaterialTheme.shapes.small
            ),
        colors = CardDefaults.cardColors(
            containerColor = when (step.status) {
                StepStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step.status) {
                    StepStatus.COMPLETED -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    StepStatus.RUNNING -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StepStatus.FAILED -> Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    StepStatus.PENDING -> Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = "Pending",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = step.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = when (step.status) {
                        StepStatus.COMPLETED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = step.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AgentTerminalOutput(
    logs: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(Color(0xFF0F0F12))
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        val terminalScrollState = rememberScrollState()
        
        // Auto-scroll terminal when logs change
        LaunchedEffect(logs) {
            terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
        }

        Text(
            text = logs,
            color = Color(0xFF00FF66), // Retro hacker green theme
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(terminalScrollState)
        )
    }
}

@Composable
fun AgentChangedFiles(
    files: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "FILES MODIFIED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            files.forEach { file ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit file icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = file,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AgentControlBar(
    task: AgentTask,
    isExecuting: Boolean,
    onApprove: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onApprove,
            enabled = !isExecuting && task.status == StepStatus.RUNNING,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("approve_step_button"),
            shape = MaterialTheme.shapes.small
        ) {
            if (isExecuting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Default.Check, "Approve")
                Spacer(modifier = Modifier.width(6.dp))
                val isDone = task.steps.all { it.status == StepStatus.COMPLETED }
                Text(if (isDone) "Task Completed" else "Approve Next Step", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("cancel_task_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(Icons.Default.Stop, "Cancel", tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear Session")
        }
    }
}
