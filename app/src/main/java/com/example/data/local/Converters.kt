package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.SyncStatus

class Converters {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return try {
            SyncStatus.valueOf(value)
        } catch (_: Exception) {
            SyncStatus.PENDING_UPLOAD
        }
    }
}
