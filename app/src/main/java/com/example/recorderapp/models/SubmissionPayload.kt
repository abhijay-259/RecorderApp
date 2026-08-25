package com.example.recorderapp.models

import kotlinx.serialization.Serializable

@Serializable
data class SubmissionPayload(val task_id: Int,
                             val worker_name: String,
                             val recorded_text: String)