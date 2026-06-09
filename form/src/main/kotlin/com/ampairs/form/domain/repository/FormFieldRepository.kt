package com.ampairs.form.domain.repository

import com.ampairs.form.domain.model.FormField
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** Field repository — members of the FormSchema aggregate. `@TenantId`-filtered. */
@Repository
interface FormFieldRepository : JpaRepository<FormField, Long> {

    fun findByEntityTypeOrderByDisplayOrderAsc(entityType: String): List<FormField>

    fun findByUid(uid: String): FormField?

    fun deleteByUidIn(uids: Collection<String>)
}
