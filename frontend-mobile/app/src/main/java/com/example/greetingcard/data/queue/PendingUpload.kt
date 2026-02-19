package com.example.greetingcard.data.queue

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class UploadStatus { PENDING, UPLOADING, DONE, FAILED }

class UploadStatusConverter {
    @TypeConverter fun fromStatus(status: UploadStatus): String = status.name
    @TypeConverter fun toStatus(name: String): UploadStatus = UploadStatus.valueOf(name)
}

@Entity(tableName = "pending_uploads")
data class PendingUpload(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val timestamp: String,
    val status: UploadStatus = UploadStatus.PENDING
)
