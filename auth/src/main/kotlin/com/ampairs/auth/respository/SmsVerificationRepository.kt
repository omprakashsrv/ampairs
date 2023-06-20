package com.ampairs.auth.respository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SmsVerificationRepository : CrudRepository<com.ampairs.auth.domain.model.SmsVerification, String>