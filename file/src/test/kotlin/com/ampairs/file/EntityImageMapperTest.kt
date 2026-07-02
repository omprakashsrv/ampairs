package com.ampairs.file

import com.ampairs.file.domain.dto.asEntityImageListResponse
import com.ampairs.file.domain.dto.asEntityImageResponse
import com.ampairs.file.domain.dto.formatEntityFileSize
import com.ampairs.file.domain.dto.toFileResponse
import com.ampairs.file.domain.model.EntityImage
import com.ampairs.file.domain.model.EntityImageMetadata
import com.ampairs.file.domain.model.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntityImageMapperTest {

    private fun image(block: EntityImage.() -> Unit = {}): EntityImage =
        EntityImage().apply {
            uid = "EIM-1"
            entityType = "customer"
            entityUid = "CUS-1"
            originalFilename = "pic.jpg"
            fileExtension = "jpg"
            contentType = "image/jpeg"
            fileSize = 2048
            checksum = "abc"
            metadata = EntityImageMetadata(etag = "et", width = 100, height = 80)
            block()
        }

    @Test
    fun `formatEntityFileSize renders human readable sizes`() {
        assertEquals("512 B", formatEntityFileSize(512))
        assertEquals("1.00 KB", formatEntityFileSize(1024))
        assertEquals("1.00 MB", formatEntityFileSize(1024L * 1024))
        assertEquals("1.00 GB", formatEntityFileSize(1024L * 1024 * 1024))
    }

    @Test
    fun `asEntityImageResponse builds urls and copies metadata`() {
        val resp = image { isPrimary = true; displayOrder = 3 }.asEntityImageResponse("/file/v1")

        assertEquals("EIM-1", resp.uid)
        assertEquals("/file/v1/images/EIM-1/download", resp.imageUrl)
        assertEquals("/file/v1/images/EIM-1/thumbnail", resp.thumbnailUrl)
        assertEquals("2.00 KB", resp.formattedFileSize)
        assertEquals(100, resp.width)
        assertEquals(80, resp.height)
        assertEquals("et", resp.etag)
        assertTrue(resp.isPrimary)
    }

    @Test
    fun `asEntityImageListResponse aggregates totals and primary`() {
        val a = image { uid = "A"; fileSize = 1000; isPrimary = false }
        val b = image { uid = "B"; fileSize = 2000; isPrimary = true }

        val list = listOf(a, b).asEntityImageListResponse("/file/v1")

        assertEquals(2, list.totalCount)
        assertEquals(3000, list.totalSize)
        assertNotNull(list.primaryImage)
        assertEquals("B", list.primaryImage!!.uid)
    }

    @Test
    fun `entity image storage path is workspace scoped`() {
        val img = image { ownerId = "WSP-1" }
        assertEquals("WSP-1/customer/CUS-1/EIM-1.jpg", img.storagePath())
    }

    @Test
    fun `file toFileResponse builds download url`() {
        val f = File().apply { uid = "FIL-1"; name = "doc.pdf"; bucket = "b"; objectKey = "k" }
        val resp = f.toFileResponse()

        assertEquals("FIL-1", resp.id)
        assertEquals("doc.pdf", resp.name)
        assertEquals("/file/v1/FIL-1/download", resp.downloadUrl)
        assertNull(resp.refId?.ifEmpty { null })
        assertEquals(1, listOf(f).toFileResponse().size)
    }
}
