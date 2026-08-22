package com.example.recorderapp.repositories

import android.media.MediaRecorder
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Dao
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.recorderapp.models.SubmissionPayload
import com.example.recorderapp.models.UserSessionProfile
import com.example.recorderapp.room.Submission
import com.example.recorderapp.room.Submission2
import com.example.recorderapp.room.SubmissionDao
import com.example.recorderapp.room.SubmissionDatabase
import com.example.recorderapp.workers.AutoSubmissionsWorker
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext

class AudioRepository(
    private val context: Context,
    private val dao: SubmissionDao
) {
    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var currentFileName: String? = null

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
    }

    fun startRecording() {
        try {
            currentFileName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString()}.mp4"
            val file = File(context.filesDir, currentFileName!!)
            /*
            Uses native built in library called android.os.Build
            Build.VERSION.SDK_INT reads numerical android version on phone
            Build.VERSION_CODES.S is constant representing Android 12 (API 31)
            */
            recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)

                prepare()
                /*
                tests connection, checks if chip is busy
                initializes AAC compression encoder software engines
                creates file container named my_voice.mp4 on storage drive
                gets the write-pointer ready
                */
                start()
                /*
                mic channel opens immediately
                physical soundwaves hitting mic are converted to electrical signals
                signals are compressed by AAC encoder
                And streamed as raw binary bytes directly onto phone's storage cache file
                */
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Prints in logcat
        }
    }
    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startPlayback(file: File) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    suspend fun startUploading(currentUser: UserSessionProfile?, isConnected: Boolean, submissionDao: SubmissionDao): Boolean {
        // 1. Locate the physical file and read its bytes
        val voiceFile = File(context.filesDir, currentFileName.toString())
        if (!voiceFile.exists()) {
            Log.e("UPLOAD_ERROR", "Physical file does not exist at: ${voiceFile.absolutePath}")
            return false
        }
        val fileBytes = voiceFile.readBytes()
        if (isConnected) {
            try {
                println("reached try isConnected = $isConnected")
                // 2. Stream the binary file to your laptop over Wi-Fi
                val response: HttpResponse = client.submitFormWithBinaryData(
                    url = "http://$ip:8000/upload-audio",
                    formData = formData {
                        append("worker_name", currentUser!!.name)
                        append("email",currentUser.email)
                        append("user_id", currentUser.id)
                        append("recorded_text", currentFileName!!)
                        append("audio_file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$currentFileName\"")
                        })
                    }
                )
                println("Response code block worked")
                if (response.status.value == 200) {
                    File(context.filesDir, currentFileName.toString()).delete()
                    println("SUccessful response")
                    currentFileName = null
                }
                return response.status.value == 200
            } catch (e: Exception) {
                println("Failed response")
                Log.e("NETWORK_ERROR", "Upload failed: ${e.localizedMessage}", e)
                return false
            }
        } else {
            try {
                val userSubmission = Submission2(
                    currentUser!!.name,
                    102,
                    voiceFile.absolutePath,
                    currentUser.id,
                )
                submissionDao.insertSubmission(userSubmission)
                triggerAutomatedUpload()
                currentFileName = null
                return true
            }
            catch (e: Exception) {
                Log.e("LOCAL STORAGE ERROR", "Save Failed: ${e.localizedMessage}", e)
                return false
            }
        }
    }
    suspend fun uploadSubmission(name: String, email: String, id: Int, fileName: String, fileBytes: ByteArray): Boolean {
        return try {
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "http://$ip:8000/upload-audio",
                formData = formData {
                    append("worker_name", name)
                    append("email", email)
                    append("user_id", id)
                    append("recorded_text", fileName)
                    append("audio_file", fileBytes, Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            ) {
                timeout {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 5000
                }
            }
            response.status.value == 200
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun uploadPendingSubmissions(): Boolean {
        Log.println(Log.ASSERT, "Entered", "uploadPendingSubmissions in audiorepo")
        return try {
            Log.println(Log.ASSERT, "Entered", "try block in function")
            val submissions = dao.getSubmissionsList()
            if (submissions.isEmpty()) return true
            for (submission in submissions) {
                Log.println(Log.ASSERT, "Entered", "for loop")
                Log.println(Log.ASSERT,"entered","Filepath is ${submission.filePath}")
                println(submission.filePath)
                val file = if (submission.filePath.startsWith("/")) {
                    File(submission.filePath)
                } else {
                    File(context.filesDir, submission.filePath)
                }
                if (!file.exists()) {
                    dao.deleteSubmission(submission.submission_id)
                    continue
                }

                Log.println(Log.ASSERT, "Sync", "Processing file: ${file.absolutePath}")
                val fileBytes = file.readBytes()

                val success = uploadSubmission(
                    submission.worker_name,
                    "example@email.com",
                    submission.worker_id ?: 0,
                    file.name,
                    fileBytes
                )

                if (success) {
                    // Delete physical asset FIRST to ensure atomicity
                    val isDeleted = file.delete()
                    if (isDeleted) {
                        Log.println(Log.ASSERT, "Sync", "Disk file deleted successfully.")
                        dao.deleteSubmission(submission.submission_id)
                    } else {
                        Log.println(Log.ERROR, "Sync", "Failed to clear disk file. Holding DB state.")
                        return false // Force retry safety
                    }
                } else {
                    Log.println(Log.WARN, "Sync", "Network upload failed for submission: ${submission.submission_id}")
                    return false // Halts loop to let WorkManager schedule a backoff retry
                }
            }
            true
        } catch (e: Exception) {
            Log.println(Log.ASSERT, "SyncError", "Exception in loop: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun triggerAutomatedUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<AutoSubmissionsWorker>()
            .setConstraints(constraints)
            .build()

        // ExistingWorkPolicy.KEEP ensures that if a sync worker is already running or
        // waiting for Wi-Fi, Android won't interrupt it or duplicate it.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "AUTOMATED_AUDIO_SYNC_TASK",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        Log.println(Log.ASSERT, "WorkManager", "Unique sync task registered with OS.")
    }
    companion object {
        const val ip: String = "192.168.88.10"
    }
}