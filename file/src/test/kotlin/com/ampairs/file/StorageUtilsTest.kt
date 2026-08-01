package com.ampairs.file

import com.ampairs.file.storage.parseStorageSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StorageUtilsTest {

    @Test
    fun `parses GB MB KB and raw bytes`() {
        assertEquals(2L * 1024 * 1024 * 1024, parseStorageSize("2GB"))
        assertEquals(10L * 1024 * 1024, parseStorageSize("10MB"))
        assertEquals(5L * 1024, parseStorageSize("5KB"))
        assertEquals(512L, parseStorageSize("512"))
    }

    @Test
    fun `is case insensitive`() {
        assertEquals(10L * 1024 * 1024, parseStorageSize("10mb"))
    }
}
