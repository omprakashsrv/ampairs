package com.ampairs.file.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.file.domain.dto.FileResponse
import com.ampairs.file.domain.dto.toFileResponse
import com.ampairs.file.domain.service.FileNotFoundException
import com.ampairs.file.domain.service.FileService
import com.ampairs.file.storage.ObjectStorageService
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/file/v1")
class FileController(
    private val fileService: FileService,
    private val objectStorageService: ObjectStorageService,
) {
    private val logger = LoggerFactory.getLogger(FileController::class.java)

    /**
     * Universal upload endpoint for non-image files (documents, exports, etc.).
     * For entity images use POST /file/v1/images/{entityType}/{entityUid} instead.
     */
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("entity_type") entityType: String,
        @RequestParam("entity_uid") entityUid: String,
    ): ApiResponse<FileResponse> {
        val workspace = TenantContextHolder.getCurrentTenant()
            ?: throw IllegalStateException("Workspace context not set")
        val folder = "${entityType.lowercase().trim()}/$workspace/$entityUid"
        val result = fileService.saveFile(
            bytes = file.inputStream.readAllBytes(),
            name = file.originalFilename ?: "file",
            contentType = file.contentType ?: "application/octet-stream",
            folder = folder,
        ).toFileResponse()
        logger.info("File uploaded: entityType={}, entityUid={}, file={}", entityType, entityUid, result.id)
        return ApiResponse.success(result)
    }

    /**
     * Stream file bytes directly from object storage.
     * For entity images use GET /file/v1/images/{imageUid}/download instead.
     */
    @GetMapping("/{fileUid}/download")
    fun download(@PathVariable fileUid: String): ResponseEntity<InputStreamResource> {
        return try {
            val file = fileService.getFile(fileUid)
            val stream = objectStorageService.downloadFile(file.bucket, file.objectKey)
            val contentType = file.contentType?.let { MediaType.parseMediaType(it) } ?: MediaType.APPLICATION_OCTET_STREAM
            val headers = HttpHeaders().apply {
                setContentType(contentType)
                set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${file.name}\"")
                setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
            }
            ResponseEntity.ok().headers(headers).body(InputStreamResource(stream))
        } catch (e: FileNotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Failed to download file: fileUid={}, error={}", fileUid, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
