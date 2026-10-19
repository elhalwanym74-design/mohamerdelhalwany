package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddEditDialog
import com.example.ui.tabs.CloudSyncTab
import com.example.ui.tabs.RecordsTab
import com.example.ui.tabs.SchemaTab
import com.example.ui.theme.SyncPending
import com.example.ui.theme.SyncSuccess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: SyncViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val allActiveItems by viewModel.allActiveItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val syncedCount by viewModel.syncedCount.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val showAddEditDialog by viewModel.showAddEditDialog.collectAsStateWithLifecycle()
    val editingItem by viewModel.editingItem.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "كلاود سينك",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "كلاود سينك",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "قاعدة بيانات Room محلياً ⇄ مزامنة Cloud Firestore",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (syncState.isFirebaseConnected) SyncSuccess else SyncPending,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (syncState.isFirebaseConnected) "متصل بالسحابة" else "تخزين محلي",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                viewModel.syncNow()
                                scope.launch {
                                    snackbarHostState.showSnackbar("تم بدء دورة المزامنة السحابية")
                                }
                            },
                            modifier = Modifier.testTag("topbar_sync_action")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "مزامنة الآن",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.ListAlt else Icons.Outlined.ListAlt,
                                contentDescription = "السجلات"
                            )
                        },
                        label = { Text("السجلات") },
                        modifier = Modifier.testTag("nav_records_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Default.CloudSync else Icons.Outlined.CloudSync,
                                contentDescription = "المزامنة السحابية"
                            )
                        },
                        label = {
                            Text(if (pendingCount > 0) "المزامنة ($pendingCount)" else "المزامنة السحابية")
                        },
                        modifier = Modifier.testTag("nav_sync_tab")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Default.Code else Icons.Outlined.Code,
                                contentDescription = "نموذج البيانات"
                            )
                        },
                        label = { Text("نموذج البيانات") },
                        modifier = Modifier.testTag("nav_schema_tab")
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.openCreateDialog() },
                    modifier = Modifier.testTag("fab_add_item"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة سجل جديد"
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_animation"
                ) { currentTab ->
                    when (currentTab) {
                        0 -> RecordsTab(
                            items = items,
                            totalCount = allActiveItems.size,
                            syncedCount = syncedCount,
                            pendingCount = pendingCount,
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.onSearchQueryChanged(it) },
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = { viewModel.onCategorySelected(it) },
                            onToggleCompleted = { viewModel.toggleCompleted(it) },
                            onEdit = { viewModel.openEditDialog(it) },
                            onDelete = { viewModel.deleteItem(it) },
                            onInspect = {
                                viewModel.selectTab(2)
                            }
                        )
                        1 -> CloudSyncTab(
                            syncState = syncState,
                            pendingCount = pendingCount,
                            syncLogs = syncLogs,
                            onSyncNow = { viewModel.syncNow() },
                            onSimulateRemote = {
                                viewModel.simulateRemoteIncomingChange()
                                scope.launch {
                                    snackbarHostState.showSnackbar("تمت محاكاة وصول تعديل سحابي جديد!")
                                }
                            }
                        )
                        2 -> SchemaTab(
                            sampleItem = items.firstOrNull() ?: allActiveItems.firstOrNull()
                        )
                    }
                }
            }
        }

        if (showAddEditDialog) {
            AddEditDialog(
                itemToEdit = editingItem,
                onDismiss = { viewModel.dismissAddEditDialog() },
                onSave = { title, desc, cat ->
                    viewModel.saveItem(title, desc, cat)
                    scope.launch {
                        snackbarHostState.showSnackbar("تم حفظ '$title' في قاعدة البيانات")
                    }
                }
            )
        }
    }
}
