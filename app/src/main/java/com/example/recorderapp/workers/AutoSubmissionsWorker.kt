package com.example.recorderapp.workers

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.recorderapp.repositories.AudioRepository
import com.example.recorderapp.room.SubmissionDao
import com.example.recorderapp.room.SubmissionDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoSubmissionsWorker(
    private val context: Context,
    private val params: WorkerParameters
): CoroutineWorker(appContext = context, params = params) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val db = SubmissionDatabase.getDatabase(applicationContext)
                val audioRepository = AudioRepository(applicationContext, db.dao)

                val isSubmissionsComplete = audioRepository.uploadPendingSubmissions()

                if (isSubmissionsComplete) {
                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}