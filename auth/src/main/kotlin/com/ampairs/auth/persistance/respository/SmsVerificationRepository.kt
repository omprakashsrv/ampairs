package com.ampairs.auth.persistance.respository

import com.ampairs.auth.domain.model.SmsVerification
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SmsVerificationRepository : CrudRepository<SmsVerification, String>