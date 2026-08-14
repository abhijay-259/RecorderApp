package com.example.recorderapp.room

import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase

@Database(entities = [Submission::class], version = 1)
abstract class SubmissionDatabase(): RoomDatabase() {
    abstract val dao: SubmissionDao
}
