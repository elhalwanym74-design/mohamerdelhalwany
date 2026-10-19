package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SyncEntity
import com.example.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_records WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllActiveFlow(): Flow<List<SyncEntity>>

    @Query("SELECT * FROM sync_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SyncEntity?

    @Query("SELECT * FROM sync_records WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncRecords(): List<SyncEntity>

    @Query("SELECT COUNT(*) FROM sync_records WHERE syncStatus != 'SYNCED' AND isDeleted = 0")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SyncEntity>)

    @Query("UPDATE sync_records SET isDeleted = 1, syncStatus = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(
        id: String,
        status: SyncStatus = SyncStatus.PENDING_DELETE,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM sync_records WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE sync_records SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM sync_records")
    suspend fun clearAll()
}
