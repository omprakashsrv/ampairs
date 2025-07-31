package com.ampairs.auth.service

import com.ampairs.auth.model.LoginSession
import com.ampairs.auth.repository.LoginSessionRepository
import com.ampairs.core.utils.UniqueIdGenerators
import io.awspring.cloud.sns.sms.SnsSmsTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OtpService @Autowired constructor(
    private val loginSessionRepository: LoginSessionRepository,
    private val snsSmsTemplate: SnsSmsTemplate,
) {
    companion object {
        const val OTP_LENGTH = 6
        const val OTP_VALIDITY_MINUTES = 10L
        const val MAX_OTP_ATTEMPTS = 3
    }

    @Transactional
    fun generateAndSendOtp(phone: String, countryCode: Int): String {
        // Invalidate any existing sessions for this phone
        invalidateExistingSessions(phone, countryCode)

        val loginSession = LoginSession().apply {
            this.phone = phone
            this.countryCode = countryCode
            this.code = UniqueIdGenerators.NUMERIC.generate(OTP_LENGTH)
            this.expiresAt = LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES)
        }

        val savedSession = loginSessionRepository.save(loginSession)

        // Send OTP via SMS
        sendOtpSms(countryCode, phone, loginSession.code)

        return savedSession.seqId
    }

    @Transactional
    fun verifyOtp(sessionId: String, otp: String): LoginSession {
        val session = loginSessionRepository.findBySeqId(sessionId)
            ?: throw IllegalArgumentException("Invalid session")

        validateOtpSession(session, otp)

        // Mark session as verified
        session.verified = true
        session.verifiedAt = LocalDateTime.now()

        return loginSessionRepository.save(session)
    }

    private fun validateOtpSession(session: LoginSession, otp: String) {
        if (session.verified) {
            throw IllegalStateException("OTP already verified")
        }

        if (session.expiresAt?.isBefore(LocalDateTime.now()) == true) {
            throw IllegalStateException("OTP expired")
        }

        if (session.attempts >= MAX_OTP_ATTEMPTS) {
            throw IllegalStateException("Maximum OTP attempts exceeded")
        }

        // Increment attempts
        session.attempts++
        loginSessionRepository.save(session)

        if (session.code != otp) {
            throw IllegalArgumentException("Invalid OTP")
        }
    }

    private fun invalidateExistingSessions(phone: String, countryCode: Int) {
        val existingSessions = loginSessionRepository.findByPhoneAndCountryCodeAndVerifiedFalse(phone, countryCode)
        existingSessions.forEach { it.expired = true }
        loginSessionRepository.saveAll(existingSessions)
    }

    private fun sendOtpSms(countryCode: Int, phone: String, otp: String) {
        val phoneNumber = "+$countryCode$phone"
        val message =
            "$otp is your one-time password to verify your phone number. Valid for $OTP_VALIDITY_MINUTES minutes."
        snsSmsTemplate.send(phoneNumber, message)
    }
}