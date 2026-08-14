package com.example.recorderapp.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Submission(
    val worker_name: String,
    val task_id: Int,
    val recorded_text: ByteArray,
    val worker_id: Int?,
    @PrimaryKey(autoGenerate = true)
    val submission_id: Int?

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Submission

        if (task_id != other.task_id) return false
        if (worker_name != other.worker_name) return false
        if (!recorded_text.contentEquals(other.recorded_text)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = task_id
        result = 31 * result + worker_name.hashCode()
        result = 31 * result + recorded_text.contentHashCode()
        return result
    }
}
