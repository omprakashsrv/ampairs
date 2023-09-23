package com.ampairs.core.domain.service

import com.ampairs.core.domain.model.File
import com.ampairs.core.respository.FileRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse


@Service
class FileService @Autowired constructor(
    val s3Client: S3Client,
    val fileRepository: FileRepository,
) {

    fun saveFile(bytes: ByteArray, name: String, objectKey: String, bucket: String, contentType: String): File {
        val putObjectResult: PutObjectResponse = s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes)
        )
        val file = File()
        file.name = name
        file.objectKey = objectKey
        file.bucket = bucket
        return fileRepository.save(file)
    }

}