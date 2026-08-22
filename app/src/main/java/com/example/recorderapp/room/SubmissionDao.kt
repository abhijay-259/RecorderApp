package com.example.recorderapp.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionDao {
    @Upsert
    suspend fun insertSubmission(submission: Submission2)

    @Query("SELECT * FROM Submission2")
    fun getSubmissions(): Flow<List<Submission2>>

    @Query("SELECT * FROM Submission2")
    suspend fun getSubmissionsList(): List<Submission2>

    @Query("DELETE FROM Submission2 WHERE submission_id = :id")
    suspend fun deleteSubmission(id: Int?)

}