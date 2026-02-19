package com.example.greetingcard.data.queue

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [PendingUpload::class], version = 1)
@TypeConverters(UploadStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingUploadDao(): PendingUploadDao
}
