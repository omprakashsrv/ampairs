package com.ampairs.form.domain.repository

import com.ampairs.form.domain.model.FormSection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** Section repository — members of the FormSchema aggregate. `@TenantId`-filtered. */
@Repository
interface FormSectionRepository : JpaRepository<FormSection, Long> {

    fun findByEntityTypeOrderByDisplayOrderAsc(entityType: String): List<FormSection>

    fun findByUid(uid: String): FormSection?

    fun deleteByUidIn(uids: Collection<String>)
}
