package com.example.recorderapp.repositories

import android.media.MediaRecorder
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.room.Dao
import com.example.recorderapp.models.SubmissionPayload
import com.example.recorderapp.models.UserSessionProfile
import com.example.recorderapp.room.Submission
import com.example.recorderapp.room.SubmissionDao
import com.example.recorderapp.room.SubmissionDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext

class AudioRepository(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
            })
        }
    }

    fun startRecording(): String? {
        try {
            val fileName = "${System.currentTimeMillis()}${UUID.randomUUID().toString()}.mp4"
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
                setOutputFile("${context.filesDir.absolutePath}/$fileName")

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
            return fileName
        } catch (e: Exception) {
            e.printStackTrace()
            // Prints in logcat
            return null
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

    suspend fun startUploading(currentUser: UserSessionProfile?, isConnected: Boolean, submissionDao: SubmissionDao, fileName: String): Boolean {
        // 1. Locate the physical file and read its bytes
        val voiceFile = File(context.filesDir, fileName)
        val fileBytes = voiceFile.readBytes()
        if (isConnected) {
            try {
                // 2. Stream the binary file to your laptop over Wi-Fi
                val response: HttpResponse = client.submitFormWithBinaryData(
                    url = "http://192.168.88.5:8000/upload-audio",
                    formData = formData {
                        append("worker_name", currentUser!!.name)
                        append("email",currentUser.email)
                        append("user_id", currentUser.id)
                        append("audio_file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                )
                if (response.status.value == 200) {
                    File(context.filesDir, fileName).delete()
                }
                return response.status.value == 200
            } catch (e: Exception) {
                Log.e("NETWORK_ERROR", "Upload failed: ${e.localizedMessage}", e)
                return false
            }
        } else {
            try {
                val userSubmission = Submission(
                    currentUser!!.name,
                    102,
                    fileBytes,
                    currentUser.id,
                    null
                )
                submissionDao.insertSubmission(userSubmission)
                File(context.externalCacheDir, "my_voice.mp4").delete()
                return true
            }
            catch (e: Exception) {
                Log.e("LOCAL STORAGE ERROR", "Save Failed: ${e.localizedMessage}", e)
                return false
            }
        }
    }

}