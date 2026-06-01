package com.ampairs.file.domain.dto

import com.ampairs.file.domain.model.File

data class FileResponse(
    val id: String = "",
    var refId: String? = "",
    val name: String = "",
    val bucket: String = "",
    val objectKey: String = "",
    val downloadUrl: String = "",
    val thumbnailUrl: String = "",
)

fun File.toFileResponse(): FileResponse = FileResponse(
    id = uid,
    name = name,
    bucket = bucket,
    objectKey = objectKey,
    downloadUrl = "/file/v1/files/$uid/download",
    thumbnailUrl = "/file/v1/files/$uid/thumbnail",
)

fun List<File>.toFileResponse(): List<FileResponse> {
    return map { it.toFileResponse() }
}
