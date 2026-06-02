package com.ampairs.file.storage

fun parseStorageSize(sizeStr: String): Long {
    val size = sizeStr.uppercase()
    return when {
        size.endsWith("GB") -> size.dropLast(2).toLong() * 1024 * 1024 * 1024
        size.endsWith("MB") -> size.dropLast(2).toLong() * 1024 * 1024
        size.endsWith("KB") -> size.dropLast(2).toLong() * 1024
        else -> size.toLong()
    }
}
