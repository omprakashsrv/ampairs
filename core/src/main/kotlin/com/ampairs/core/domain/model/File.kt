package com.ampairs.core.domain.model

import com.ampairs.core.config.Constants
import jakarta.persistence.Column
import jakarta.persistence.Entity


@Entity(name = "file")
class File : OwnableBaseDomain() {

    @Column(name = "name", nullable = false, length = 255)
    var name: String = ""

    @Column(name = "bucket", nullable = false, length = 255)
    var bucket: String = ""

    @Column(name = "object_key", nullable = false, length = 500)
    var objectKey: String = ""

    @Column(name = "content_type", length = 100)
    var contentType: String? = null

    @Column(name = "size")
    var size: Long? = null

    @Column(name = "etag", length = 100)
    var etag: String? = null

    override fun obtainSeqIdPrefix(): String {
        return Constants.FILE_ID_PREFIX
    }
}