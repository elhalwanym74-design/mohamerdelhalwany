package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.SyncEntity
import com.example.data.model.SyncStatus
import com.example.data.remote.FirestoreSyncManager
import com.example.data.remote.SyncState
import com.example.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SyncLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val isSuccess: Boolean = true
)

class SyncViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val syncDao = database.syncDao()
    private val syncManager = FirestoreSyncManager(application, syncDao, viewModelScope)
    private val repository = SyncRepository(syncDao, syncManager)

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _showAddEditDialog = MutableStateFlow(false)
    val showAddEditDialog: StateFlow<Boolean> = _showAddEditDialog.asStateFlow()

    private val _editingItem = MutableStateFlow<SyncEntity?>(null)
    val editingItem: StateFlow<SyncEntity?> = _editingItem.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<SyncLogEntry>>(emptyList())
    val syncLogs: StateFlow<List<SyncLogEntry>> = _syncLogs.asStateFlow()

    val syncState: StateFlow<SyncState> = repository.syncState

    val pendingCount: StateFlow<Int> = repository.pendingCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val allActiveItems: StateFlow<List<SyncEntity>> = repository.activeItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val items: StateFlow<List<SyncEntity>> = combine(
        repository.activeItems,
        _searchQuery,
        _selectedCategory
    ) { allItems, query, category ->
        allItems.filter { item ->
            val matchesCategory = category == "الكل" || item.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val syncedCount: StateFlow<Int> = repository.activeItems.combine(_searchQuery) { all, _ ->
        all.count { it.syncStatus == SyncStatus.SYNCED }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val categories: StateFlow<List<String>> = repository.activeItems.combine(_selectedCategory) { allItems, _ ->
        val cats = allItems.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        listOf("الكل") + cats
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("الكل")
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            addLog("تم تهيئة النظام. قاعدة بيانات Room SQLite المحلية جاهزة.")
            addLog("تم تسجيل مستمع التغييرات اللحظية لمجموعة sync_records.")
        }
    }

    private fun addLog(message: String, isSuccess: Boolean = true) {
        val entry = SyncLogEntry(message = message, isSuccess = isSuccess)
        _syncLogs.value = (listOf(entry) + _syncLogs.value).take(20)
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun openCreateDialog() {
        _editingItem.value = null
        _showAddEditDialog.value = true
    }

    fun openEditDialog(item: SyncEntity) {
        _editingItem.value = item
        _showAddEditDialog.value = true
    }

    fun dismissAddEditDialog() {
        _showAddEditDialog.value = false
        _editingItem.value = null
    }

    fun saveItem(title: String, description: String, category: String) {
        viewModelScope.launch {
            val current = _editingItem.value
            if (current != null) {
                repository.updateItem(
                    existing = current,
                    newTitle = title,
                    newDescription = description,
                    newCategory = category,
                    newCompleted = current.isCompleted
                )
                addLog("تم تحديث السجل المحلي '${title.take(20)}' (بانتظار الرفع)")
            } else {
                repository.addItem(
                    title = title,
                    description = description,
                    category = category
                )
                addLog("تم إنشاء سجل محلي جديد '${title.take(20)}' (بانتظار المزامنة)")
            }
            dismissAddEditDialog()
        }
    }

    fun toggleCompleted(item: SyncEntity) {
        viewModelScope.launch {
            repository.toggleCompleted(item)
            val status = if (!item.isCompleted) "مكتمل" else "نشط"
            addLog("تم تعديل حالة '${item.title.take(20)}' إلى $status")
        }
    }

    fun deleteItem(item: SyncEntity) {
        viewModelScope.launch {
            repository.deleteItem(item.id)
            addLog("تم تعليم السجل للحذف المؤقت '${item.title.take(20)}' (PENDING_DELETE)")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            addLog("بدء دورة المزامنة السحابية...")
            repository.syncNow()
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            addLog("اكتملت المزامنة بنجاح في الساعة $time. توافقت البيانات المحلية والسحابية.")
        }
    }

    fun simulateRemoteIncomingChange() {
        viewModelScope.launch {
            val randomId = (100..999).random()
            val title = "تحديث سحابي وارد #$randomId"
            repository.simulateRemoteItem(
                title = title,
                desc = "مستند سحابي تم استقباله عبر مستمع Snapshot من جهاز آخر.",
                category = "سحابي"
            )
            addLog("تمت محاكاة وصول تغيير سحابي جديد '$title' بنجاح.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncManager.cleanup()
    }
}
