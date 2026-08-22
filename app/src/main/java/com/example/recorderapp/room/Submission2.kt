package com.example.recorderapp.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Submission2(
    val worker_name: String,
    val task_id: Int,
    val filePath: String,
    val worker_id: Int?,
    @PrimaryKey(autoGenerate = true)
    val submission_id: Int = 0
)

