package com.example.data.repository

import com.example.data.local.SyncDao
import com.example.data.model.SyncEntity
import com.example.data.model.SyncStatus
import com.example.data.remote.FirestoreSyncManager
import com.example.data.remote.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class SyncRepository(
    private val syncDao: SyncDao,
    private val syncManager: FirestoreSyncManager
) {
    val activeItems: Flow<List<SyncEntity>> = syncDao.getAllActiveFlow()
    val pendingCount: Flow<Int> = syncDao.getPendingCountFlow()
    val syncState: StateFlow<SyncState> = syncManager.syncState

    suspend fun addItem(
        title: String,
        description: String,
        category: String
    ): SyncEntity {
        val entity = SyncEntity(
            title = title.trim(),
            description = description.trim(),
            category = category.trim().ifEmpty { "عام" },
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        syncDao.upsert(entity)
        return entity
    }

    suspend fun updateItem(
        existing: SyncEntity,
        newTitle: String,
        newDescription: String,
        newCategory: String,
        newCompleted: Boolean
    ) {
        val updated = existing.copy(
            title = newTitle.trim(),
            description = newDescription.trim(),
            category = newCategory.trim().ifEmpty { "عام" },
            isCompleted = newCompleted,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        syncDao.upsert(updated)
    }

    suspend fun toggleCompleted(item: SyncEntity) {
        val updated = item.copy(
            isCompleted = !item.isCompleted,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        syncDao.upsert(updated)
    }

    suspend fun deleteItem(id: String) {
        syncDao.markDeleted(id, SyncStatus.PENDING_DELETE, System.currentTimeMillis())
    }

    suspend fun syncNow() {
        syncManager.syncNow()
    }

    suspend fun simulateRemoteItem(title: String, desc: String, category: String) {
        syncManager.simulateRemoteSyncItem(title, desc, category)
    }

    suspend fun seedInitialDataIfEmpty() {
        val current = syncDao.getAllActiveFlow().first()
        if (current.isEmpty()) {
            val sampleItems = listOf(
                SyncEntity(
                    title = "إعداد قواعد أمان Cloud Firestore",
                    description = "تأمين قراءة وكتابة المستندات في مجموعة sync_records للمستخدمين المصادقين.",
                    category = "سحابي",
                    isCompleted = true,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis() - 3600_000
                ),
                SyncEntity(
                    title = "تصميم نموذج Room SQLite",
                    description = "هيكلة الجداول مع مفاتيح UUID لدعم العمل أوفلاين بدون انترنت وبدون تعارض في المعرفات.",
                    category = "بنية النظام",
                    isCompleted = false,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    updatedAt = System.currentTimeMillis() - 1800_000
                ),
                SyncEntity(
                    title = "محرك المزامنة الثنائي (Offline-First)",
                    description = "الكتابة فوراً محلياً في SQLite ثم المزامنة عبر مستمع Snapshot مع خوارزمية Last-Write-Wins.",
                    category = "بدون اتصال",
                    isCompleted = false,
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = System.currentTimeMillis() - 900_000
                )
            )
            syncDao.upsertAll(sampleItems)
        }
    }
}
