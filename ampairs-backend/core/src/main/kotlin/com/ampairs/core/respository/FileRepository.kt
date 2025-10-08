package com.ampairs.core.respository

import com.ampairs.core.domain.model.File
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface FileRepository : CrudRepository<File, Int> {

}