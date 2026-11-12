package com.example.recorderapp.repositories

import android.media.MediaRecorder
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.recorderapp.models.UserSessionProfile
import com.example.recorderapp.room.Submission2
import com.example.recorderapp.room.SubmissionDao
import com.example.recorderapp.workers.AutoSubmissionsWorker
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException
import java.util.UUID

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
            currentFileName?.let { File(context.filesDir, it).delete() }   // clean up the previous unsaved recording
            currentFileName = null

            val name = "${System.currentTimeMillis()}_${UUID.randomUUID()}.mp4"
            currentFileName = name
            val file = File(context.filesDir, name)
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

    fun cancelRecording() {
        currentFileName?.let {
            val deleted = File(context.filesDir, it).delete()
            Log.d("CANCEL_RECORDING", "Deleted $it: $deleted")
        }
        currentFileName = null
    }

    fun startPlayback(file: File, onComplete: () -> Unit): Boolean {
        stopPlayback()
        var started = false
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(context, Uri.fromFile(file))
                setOnCompletionListener { stopPlayback(); onComplete() }
                prepare()
                start()
                started = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (!started) stopPlayback()
        return started
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
        val fileName = currentFileName
        if (fileName == null) {
            Log.e("UPLOAD_ERROR", "No recording to upload")
            return false
        }
        // 1. Locate the physical file and read its bytes
        val voiceFile = File(context.filesDir, currentFileName.toString())
        if (!voiceFile.exists()) {
            Log.e("UPLOAD_ERROR", "Physical file does not exist at: ${voiceFile.absolutePath}")
            return false
        }
        val fileBytes = voiceFile.readBytes()
        if (isConnected) {
            val token = getIdToken()
            if (token ==null) {
                Log.e("UPLOAD_ERROR", "No ID token available, user may be signed out")
                return false
            }
            try {
                println("reached try isConnected = $isConnected")
                // 2. Stream the binary file to your laptop over Wi-Fi
                val response: HttpResponse = client.submitFormWithBinaryData(
                    url = "http://$IP:8000/upload-audio",
                    formData = formData {
                        append("worker_name", currentUser!!.name)
                        append("email",currentUser.email)
                        append("user_id", currentUser.id)
                        append("recorded_text", fileName)
                        append("audio_file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })

                    }
                ) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                if (response.status.value != 200) {
                    Log.e("UPLOAD_ERROR", "Status ${response.status.value}: ${response.bodyAsText()}")
                }
                if (response.status.value == 200) {
                    File(context.filesDir, fileName.toString()).delete()
                    currentFileName = null
                }
                return response.status.value == 200
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: Exception) {
                println("Failed response")
                Log.e("NETWORK_ERROR", "Upload failed: ${e.localizedMessage}", e)
                return false
            }
        } else {
            try {
                val userSubmission = Submission2(
                    currentUser!!.name,
                    DEFAULT_TASK_ID,
                    voiceFile.absolutePath,
                    currentUser.id,
                )
                submissionDao.insertSubmission(userSubmission)
                triggerAutomatedUpload()
                currentFileName = null
                return true
            }
            catch (e: CancellationException) {
                throw e
            }
            catch (e: Exception) {
                Log.e("LOCAL STORAGE ERROR", "Save Failed: ${e.localizedMessage}", e)
                return false
            }
        }
    }
    suspend fun uploadSubmission(name: String, email: String, id: String, fileName: String, fileBytes: ByteArray): Boolean {
        return try {
            val token = getIdToken() ?: return false
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "http://$IP:8000/upload-audio",
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
                header(HttpHeaders.Authorization, "Bearer $token")
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
        Log.println(Log.ASSERT, "Entered", "uploadPendingSubmissions in audioRepository")
        return try {
            Log.println(Log.ASSERT, "Entered", "try block in function")
            val submissions = dao.getSubmissionsList()
            if (submissions.isEmpty()) return true
            for (submission in submissions) {
                Log.println(Log.ASSERT, "Entered", "for loop")
                Log.println(Log.ASSERT,"entered","Filepath is ${submission.filePath}")
                println(submission.filePath)
                val file = File(submission.filePath)
                if (!file.exists()) {
                    dao.deleteSubmission(submission.submission_id)
                    continue
                }

                Log.println(Log.ASSERT, "Sync", "Processing file: ${file.absolutePath}")
                val fileBytes = file.readBytes()

                val success = uploadSubmission(
                    submission.worker_name,
                    "example@email.com",
                    submission.worker_id ?: "",
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
        }
        catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
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
        Log.println(        Log.ASSERT, "WorkManager", "Unique sync task registered with OS.")
    }
    private suspend fun getIdToken(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AUTH", "Could not get ID token: ${e.localizedMessage}")
            null
        }
    }
    companion object {
        const val IP: String = "192.168.1.10"
        const val DEFAULT_TASK_ID = 102
    }
}