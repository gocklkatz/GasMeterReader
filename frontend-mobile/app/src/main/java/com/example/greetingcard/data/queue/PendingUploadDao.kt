package com.example.greetingcard.data.queue

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingUploadDao {
    @Insert
    suspend fun insert(upload: PendingUpload): Long

    @Query("SELECT * FROM pending_uploads WHERE id = :id")
    suspend fun getById(id: Int): PendingUpload?

    @Query("UPDATE pending_uploads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String): Int
}
