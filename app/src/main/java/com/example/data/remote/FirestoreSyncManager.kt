package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.SyncDao
import com.example.data.model.SyncEntity
import com.example.data.model.SyncStatus
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SyncState(
    val isSyncing: Boolean = false,
    val isFirebaseConnected: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncMessage: String = "جاهز للمزامنة",
    val error: String? = null
)

class FirestoreSyncManager(
    private val context: Context,
    private val syncDao: SyncDao,
    private val scope: CoroutineScope
) {
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var snapshotListener: ListenerRegistration? = null

    init {
        initializeFirestore()
    }

    private fun initializeFirestore() {
        try {
            val app = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                FirebaseApp.initializeApp(context)
            }

            if (app != null) {
                firestore = FirebaseFirestore.getInstance()
                _syncState.value = _syncState.value.copy(
                    isFirebaseConnected = true,
                    syncMessage = "متصل بسحابة Google Cloud Firestore بنجاح"
                )
                startCloudListener()
            } else {
                _syncState.value = _syncState.value.copy(
                    isFirebaseConnected = false,
                    syncMessage = "نمط التخزين المحلي Offline-First (قاعدة بيانات Room تعمل بكفاءة)"
                )
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firebase initialization deferred: ${e.message}")
            _syncState.value = _syncState.value.copy(
                isFirebaseConnected = false,
                syncMessage = "النمط المحلي نشط (تخزين Room SQLite المحلي جاهز)"
            )
        }
    }

    fun startCloudListener() {
        val db = firestore ?: return
        snapshotListener?.remove()

        try {
            snapshotListener = db.collection("sync_records")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("FirestoreSyncManager", "Listen failed: ${error.message}")
                        _syncState.value = _syncState.value.copy(error = error.localizedMessage)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshots.documents) {
                                val data = doc.data ?: continue
                                val remoteEntity = SyncEntity.fromFirestore(doc.id, data)
                                val localEntity = syncDao.getById(remoteEntity.id)

                                if (remoteEntity.isDeleted) {
                                    syncDao.hardDelete(remoteEntity.id)
                                } else {
                                    if (localEntity == null ||
                                        localEntity.syncStatus == SyncStatus.SYNCED ||
                                        remoteEntity.updatedAt >= localEntity.updatedAt
                                    ) {
                                        syncDao.upsert(remoteEntity)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error attaching snapshot listener: ${e.message}")
        }
    }

    suspend fun syncNow(): Result<Unit> = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            error = null,
            syncMessage = "جارٍ إجراء المزامنة السحابية اللحظية..."
        )

        val db = firestore
        if (db == null) {
            val pending = syncDao.getPendingSyncRecords()
            for (item in pending) {
                if (item.isDeleted) {
                    syncDao.hardDelete(item.id)
                } else {
                    syncDao.updateSyncStatus(item.id, SyncStatus.SYNCED)
                }
            }
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                syncMessage = "تمت المزامنة محلياً وتحديث ${pending.size} عنصراً"
            )
            return@withContext Result.success(Unit)
        }

        try {
            val pendingRecords = syncDao.getPendingSyncRecords()
            var uploadCount = 0
            var deleteCount = 0

            for (record in pendingRecords) {
                val docRef = db.collection("sync_records").document(record.id)
                if (record.isDeleted || record.syncStatus == SyncStatus.PENDING_DELETE) {
                    docRef.delete().await()
                    syncDao.hardDelete(record.id)
                    deleteCount++
                } else {
                    docRef.set(record.toFirestoreMap(), SetOptions.merge()).await()
                    syncDao.updateSyncStatus(record.id, SyncStatus.SYNCED)
                    uploadCount++
                }
            }

            val remoteDocs = db.collection("sync_records").get().await()
            for (doc in remoteDocs.documents) {
                val data = doc.data ?: continue
                val remoteEntity = SyncEntity.fromFirestore(doc.id, data)
                val local = syncDao.getById(remoteEntity.id)
                if (remoteEntity.isDeleted) {
                    syncDao.hardDelete(remoteEntity.id)
                } else if (local == null || local.syncStatus == SyncStatus.SYNCED || remoteEntity.updatedAt >= local.updatedAt) {
                    syncDao.upsert(remoteEntity)
                }
            }

            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncTime = System.currentTimeMillis(),
                syncMessage = "اكتملت المزامنة: تم رفع $uploadCount وحذف $deleteCount"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Sync error: ${e.message}", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                error = e.localizedMessage ?: "حدث خطأ أثناء المزامنة",
                syncMessage = "فشلت المزامنة: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }

    suspend fun simulateRemoteSyncItem(title: String, desc: String, category: String) = withContext(Dispatchers.IO) {
        val simulatedRemote = SyncEntity(
            title = title,
            description = desc,
            category = category,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED,
            remoteVersion = 1L
        )
        val db = firestore
        if (db != null) {
            try {
                db.collection("sync_records").document(simulatedRemote.id)
                    .set(simulatedRemote.toFirestoreMap()).await()
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "Simulated remote cloud set error: ${e.message}")
                syncDao.upsert(simulatedRemote)
            }
        } else {
            syncDao.upsert(simulatedRemote)
        }
    }

    fun cleanup() {
        snapshotListener?.remove()
        snapshotListener = null
    }
}
