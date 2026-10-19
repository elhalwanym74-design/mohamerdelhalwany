package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room-like data model for local persistence and cloud synchronization with Firestore.
 * Uses UUID strings for uniform primary keys across local Room database and Cloud Firestore documents.
 */
@Entity(tableName = "sync_records")
data class SyncEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String = "General",
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val isDeleted: Boolean = false,
    val remoteVersion: Long = 0L
) {
    /**
     * Serializes this entity into a Firestore document map.
     */
    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "category" to category,
            "isCompleted" to isCompleted,
            "updatedAt" to updatedAt,
            "isDeleted" to isDeleted,
            "remoteVersion" to remoteVersion
        )
    }

    companion object {
        /**
         * Deserializes a Firestore document into a local Room SyncEntity.
         * Sets syncStatus to SYNCED because this data originates from the cloud.
         */
        fun fromFirestore(docId: String, data: Map<String, Any?>): SyncEntity {
            return SyncEntity(
                id = (data["id"] as? String) ?: docId,
                title = (data["title"] as? String) ?: "",
                description = (data["description"] as? String) ?: "",
                category = (data["category"] as? String) ?: "General",
                isCompleted = (data["isCompleted"] as? Boolean) ?: false,
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                syncStatus = SyncStatus.SYNCED,
                isDeleted = (data["isDeleted"] as? Boolean) ?: false,
                remoteVersion = (data["remoteVersion"] as? Number)?.toLong() ?: 1L
            )
        }
    }
}
