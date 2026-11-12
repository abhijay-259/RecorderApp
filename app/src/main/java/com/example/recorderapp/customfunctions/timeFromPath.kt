package com.example.recorderapp.customfunctions

import java.io.File
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

private val displayFormat = DateTimeFormatter.ofPattern("dd MM yyy, hh:mm:ss a")

fun timeFromPath(filePath: String): String? {
    val millis = File(filePath).name
        .substringBefore("_")
        .toLongOrNull() ?: return null
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(displayFormat)

}