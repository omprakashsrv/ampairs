package com.ampairs.notification.template

import com.ampairs.notification.config.NotificationProperties
import org.springframework.stereotype.Service

@Service
class SmsTemplateService(
    private val props: NotificationProperties
) {

    private val appName get() = props.sms.appName

    fun getOtpVerificationMessage(otp: String, validityMinutes: Int = 10): String {
        return "$otp is your one-time password (OTP) for $appName. Valid for $validityMinutes minutes. Do not share this OTP with anyone for security reasons."
    }

    fun getLoginOtpMessage(otp: String, validityMinutes: Int = 10): String {
        return "Your $appName login OTP is $otp. This code will expire in $validityMinutes minutes. Keep it confidential for your account security."
    }

    fun getRegistrationOtpMessage(otp: String, validityMinutes: Int = 10): String {
        return "Welcome to $appName! Your verification code is $otp. Please verify your phone number within $validityMinutes minutes to complete registration."
    }

    fun getPasswordResetOtpMessage(otp: String, validityMinutes: Int = 15): String {
        return "Your $appName password reset code is $otp. Use this code within $validityMinutes minutes to reset your password. If you didn't request this, please ignore."
    }

    fun getPhoneVerificationMessage(otp: String, validityMinutes: Int = 10): String {
        return "$otp is your phone verification code for $appName. Enter this code to verify your phone number. Valid for $validityMinutes minutes only."
    }

    fun getTwoFactorAuthMessage(otp: String, validityMinutes: Int = 5): String {
        return "Your $appName 2FA security code is $otp. This code expires in $validityMinutes minutes. Never share this code with anyone."
    }

    fun getTransactionOtpMessage(otp: String, amount: String? = null, validityMinutes: Int = 5): String {
        val amountText = amount?.let { " for ₹$it" } ?: ""
        return "Your $appName transaction verification code$amountText is $otp. This code is valid for $validityMinutes minutes. Do not share for security."
    }

    fun getSecurityAlertOtpMessage(otp: String, validityMinutes: Int = 10): String {
        return "Security Alert: Your $appName verification code is $otp. If you didn't request this, contact support immediately. Valid for $validityMinutes minutes."
    }

    fun getProfileUpdateOtpMessage(otp: String, validityMinutes: Int = 10): String {
        return "Verify your $appName profile changes with OTP: $otp. This code will expire in $validityMinutes minutes. Keep this code confidential."
    }

    fun getResendOtpMessage(otp: String, validityMinutes: Int = 10): String {
        return "As requested, your new $appName verification code is $otp. This OTP is valid for $validityMinutes minutes. Previous codes are now invalid."
    }

    fun getCustomOtpMessage(otp: String, action: String, validityMinutes: Int = 10): String {
        return "Your $appName verification code for $action is $otp. This code will expire in $validityMinutes minutes. Keep it secure."
    }

    fun getTemplateBasedOtpMessage(otp: String): String = otp

    fun getBusinessVerificationOtpMessage(otp: String, businessName: String, validityMinutes: Int = 15): String {
        return "Verify your business '$businessName' on $appName with OTP: $otp. This verification code expires in $validityMinutes minutes."
    }

    fun getEmergencyAccessOtpMessage(otp: String, validityMinutes: Int = 5): String {
        return "URGENT: Your $appName emergency access code is $otp. This high-security code expires in $validityMinutes minutes. Use immediately if authorized."
    }

    fun getOtpMessageInHindi(otp: String, validityMinutes: Int = 10): String {
        return "$otp आपका $appName वन-टाइम पासवर्ड है। यह $validityMinutes मिनट के लिए वैध है। किसी के साथ साझा न करें।"
    }

    fun getOtpMessageInGujarati(otp: String, validityMinutes: Int = 10): String {
        return "$otp તમારો $appName વન-ટાઇમ પાસવર્ડ છે. આ $validityMinutes મિનિટ માટે માન્ય છે. કોઈની સાથે શેર કરશો નહીં."
    }

    fun getOtpMessage(scenario: OtpScenario, otp: String, additionalData: Map<String, String> = emptyMap()): String {
        val validityMinutes = additionalData["validityMinutes"]?.toIntOrNull() ?: getDefaultValidityMinutes(scenario)

        return when (scenario) {
            OtpScenario.LOGIN -> getLoginOtpMessage(otp, validityMinutes)
            OtpScenario.REGISTRATION -> getRegistrationOtpMessage(otp, validityMinutes)
            OtpScenario.PASSWORD_RESET -> getPasswordResetOtpMessage(otp, validityMinutes)
            OtpScenario.PHONE_VERIFICATION -> getPhoneVerificationMessage(otp, validityMinutes)
            OtpScenario.TWO_FACTOR_AUTH -> getTwoFactorAuthMessage(otp, validityMinutes)
            OtpScenario.TRANSACTION_VERIFICATION -> getTransactionOtpMessage(
                otp,
                additionalData["amount"],
                validityMinutes
            )
            OtpScenario.SECURITY_ALERT -> getSecurityAlertOtpMessage(otp, validityMinutes)
            OtpScenario.PROFILE_UPDATE -> getProfileUpdateOtpMessage(otp, validityMinutes)
            OtpScenario.RESEND_OTP -> getResendOtpMessage(otp, validityMinutes)
            OtpScenario.BUSINESS_VERIFICATION -> getBusinessVerificationOtpMessage(
                otp,
                additionalData["businessName"] ?: "Your Business",
                validityMinutes
            )
            OtpScenario.EMERGENCY_ACCESS -> getEmergencyAccessOtpMessage(otp, validityMinutes)
            OtpScenario.GENERIC -> getOtpVerificationMessage(otp, validityMinutes)
            OtpScenario.CUSTOM -> getCustomOtpMessage(otp, additionalData["action"] ?: "verification", validityMinutes)
        }
    }

    private fun getDefaultValidityMinutes(scenario: OtpScenario): Int {
        return when (scenario) {
            OtpScenario.TWO_FACTOR_AUTH,
            OtpScenario.TRANSACTION_VERIFICATION,
            OtpScenario.EMERGENCY_ACCESS -> 5
            OtpScenario.PASSWORD_RESET,
            OtpScenario.BUSINESS_VERIFICATION -> 15
            else -> 10
        }
    }

    fun isValidOtpFormat(otp: String): Boolean = otp.matches(Regex("^\\d{6}$"))

    fun generateSecureOtp(): String = (100000..999999).random().toString()
}

enum class OtpScenario {
    LOGIN,
    REGISTRATION,
    PASSWORD_RESET,
    PHONE_VERIFICATION,
    TWO_FACTOR_AUTH,
    TRANSACTION_VERIFICATION,
    SECURITY_ALERT,
    PROFILE_UPDATE,
    RESEND_OTP,
    BUSINESS_VERIFICATION,
    EMERGENCY_ACCESS,
    GENERIC,
    CUSTOM
}
