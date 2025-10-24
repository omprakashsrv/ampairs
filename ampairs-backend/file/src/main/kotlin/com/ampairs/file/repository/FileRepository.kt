package com.ampairs.file.repository

import com.ampairs.file.domain.model.File
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface FileRepository : CrudRepository<File, Int>
