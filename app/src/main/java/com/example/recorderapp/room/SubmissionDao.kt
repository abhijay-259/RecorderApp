package com.example.recorderapp.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionDao {
    @Upsert
    suspend fun insertSubmission(submission: Submission)

    @Query("SELECT * FROM submission")
    fun getSubmissions(): Flow<List<Submission>>


}