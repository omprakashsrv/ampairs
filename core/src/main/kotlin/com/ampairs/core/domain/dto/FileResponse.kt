package com.ampairs.core.domain.dto

import com.ampairs.core.domain.model.File

data class FileResponse(
    val id: String = "",
    var refId: String? = "",
    val name: String = "",
    val bucket: String = "",
    val objectKey: String = "",
)

fun File.toFileResponse(): FileResponse {
    return FileResponse(
        id = this.seqId,
        name = this.name,
        bucket = this.bucket,
        objectKey = this.objectKey
    )
}

fun List<File>.toFileResponse(): List<FileResponse> {
    return map {
        it.toFileResponse()
    }
}