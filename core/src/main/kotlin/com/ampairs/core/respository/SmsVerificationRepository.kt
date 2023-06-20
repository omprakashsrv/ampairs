package com.ampairs.core.respository

import com.ampairs.core.domain.model.SmsVerification
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SmsVerificationRepository : CrudRepository<SmsVerification, String>