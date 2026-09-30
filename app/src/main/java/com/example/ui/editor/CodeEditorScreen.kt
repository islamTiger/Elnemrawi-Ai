package com.example.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ProjectFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    viewModel: EditorViewModel,
    projectId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // BackHandler handles popping back stack safely
    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val project by viewModel.project.collectAsState()
    val files by viewModel.files.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    var sidebarOpen by remember { mutableStateOf(true) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var createParentPath by remember { mutableStateOf("") }
    var createIsDirectory by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth > 600.dp
        // Automatically close/open explorer based on tablet screen guidelines
        LaunchedEffect(isWideScreen) {
            sidebarOpen = isWideScreen
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Text(
                            text = project?.name ?: "Editor Workspace",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { sidebarOpen = !sidebarOpen },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (sidebarOpen) Icons.Default.MenuOpen else Icons.Default.Menu,
                                contentDescription = "Toggle Explorer"
                            )
                        }

                        if (selectedFile != null) {
                            Button(
                                onClick = {
                                    viewModel.saveFile()
                                    Toast.makeText(context, "File saved successfully", Toast.LENGTH_SHORT).show()
                                },
                                enabled = hasUnsavedChanges,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("save_file_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
                )
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Left File Tree Sidebar
                if (sidebarOpen) {
                    Column(
                        modifier = Modifier
                            .width(if (isWideScreen) 280.dp else 220.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WORKSPACE FILES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            IconButton(
                                onClick = {
                                    createParentPath = ""
                                    createIsDirectory = false
                                    showCreateFileDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "New File in Root",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Search/Filter Action
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search files...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, "Search", modifier = Modifier.size(14.dp)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            textStyle = TextStyle(fontSize = 12.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Filter files based on search query
                            val filteredFiles = if (searchQuery.trim().isEmpty()) {
                                files
                            } else {
                                files.filter { recursiveSearch(it, searchQuery) }
                            }

                            items(filteredFiles) { rootNode ->
                                FileTreeNodeView(
                                    node = rootNode,
                                    level = 0,
                                    selectedPath = selectedFile?.path ?: "",
                                    onSelectFile = { viewModel.selectFile(it) },
                                    onAddFile = { parentPath, isDir ->
                                        createParentPath = parentPath
                                        createIsDirectory = isDir
                                        showCreateFileDialog = true
                                    },
                                    onDelete = { viewModel.deleteFile(it) }
                                )
                            }
                        }
                    }
                }

                // Right Code Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (selectedFile == null) {
                        EmptyEditorState(onOpenSidebar = { sidebarOpen = true })
                    } else {
                        // Header info of active file
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "File active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedFile!!.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (hasUnsavedChanges) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(MaterialTheme.shapes.extraSmall)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                            SuggestionChip(
                                onClick = {},
                                label = { Text(getFileLanguageLabel(selectedFile!!.name)) }
                            )
                        }

                        // Source code interactive text field
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // Monospace Line numbers column for professional visual layout
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                val linesCount = fileContent.lines().size.coerceAtLeast(1)
                                repeat(linesCount) { index ->
                                    Text(
                                        text = "${index + 1}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.height(20.dp)
                                    )
                                }
                            }

                            TextField(
                                value = fileContent,
                                onValueChange = { viewModel.updateFileContent(it) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("code_text_field"),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    lineHeight = 20.sp
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        // New file dialog popup
        if (showCreateFileDialog) {
            CreateFileDialog(
                parentPath = createParentPath,
                isDirectory = createIsDirectory,
                onDismiss = { showCreateFileDialog = false },
                onCreate = { name ->
                    viewModel.createNewFile(createParentPath, name, createIsDirectory)
                    showCreateFileDialog = false
                }
            )
        }
    }
}

@Composable
fun FileTreeNodeView(
    node: ProjectFile,
    level: Int,
    selectedPath: String,
    onSelectFile: (ProjectFile) -> Unit,
    onAddFile: (parentPath: String, isDirectory: Boolean) -> Unit,
    onDelete: (path: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isSelected = node.path == selectedPath

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else Color.Transparent
                )
                .clickable {
                    if (node.isDirectory) {
                        expanded = !expanded
                    } else {
                        onSelectFile(node)
                    }
                }
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .padding(start = (level * 12).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = when {
                        node.isDirectory && expanded -> Icons.Default.FolderOpen
                        node.isDirectory -> Icons.Default.Folder
                        else -> Icons.Default.Description
                    },
                    contentDescription = "Node type",
                    tint = if (node.isDirectory) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = node.name,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }

            // Quick add / delete actions for file managers
            Row {
                if (node.isDirectory) {
                    IconButton(
                        onClick = { onAddFile(node.path, false) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New File",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { onDelete(node.path) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete Node",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (node.isDirectory && expanded) {
            node.children.forEach { child ->
                FileTreeNodeView(
                    node = child,
                    level = level + 1,
                    selectedPath = selectedPath,
                    onSelectFile = onSelectFile,
                    onAddFile = onAddFile,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun EmptyEditorState(
    onOpenSidebar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = "No file open",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Active Source Code Workspace",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please open the file tree to select and edit files in real-time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenSidebar, shape = MaterialTheme.shapes.small) {
            Text("Open Explorer")
        }
    }
}

@Composable
fun CreateFileDialog(
    parentPath: String,
    isDirectory: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val typeLabel = if (isDirectory) "Directory" else "File"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New $typeLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (parentPath.isNotEmpty()) {
                    Text(
                        text = "Adding under: $parentPath",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("$typeLabel Name") },
                    placeholder = { Text(if (isDirectory) "e.g. controller" else "e.g. Route.kt") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank(),
                shape = MaterialTheme.shapes.small
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = MaterialTheme.shapes.medium
    )
}

fun recursiveSearch(node: ProjectFile, query: String): Boolean {
    if (node.name.contains(query, ignoreCase = true)) return true
    if (node.isDirectory) {
        return node.children.any { recursiveSearch(it, query) }
    }
    return false
}

fun getFileLanguageLabel(fileName: String): String {
    return when {
        fileName.endsWith(".kt") -> "Kotlin"
        fileName.endsWith(".java") -> "Java"
        fileName.endsWith(".tsx") || fileName.endsWith(".ts") -> "TypeScript"
        fileName.endsWith(".json") -> "JSON"
        fileName.endsWith(".xml") -> "XML"
        fileName.endsWith(".gradle") || fileName.endsWith(".kts") -> "Gradle DSL"
        fileName.endsWith(".py") -> "Python"
        else -> "Source"
    }
}
