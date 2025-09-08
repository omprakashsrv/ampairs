package com.ampairs.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Spring Cloud AWS Configuration Properties
 * Uses Spring Cloud AWS auto-configuration instead of manual AWS SDK setup
 */
@ConfigurationProperties(prefix = "spring.cloud.aws")
data class SpringCloudAwsProperties(
    val region: Region = Region(),
    val credentials: Credentials = Credentials(),
    val s3: S3Properties = S3Properties(),
    val sns: SnsProperties = SnsProperties(),
    val stack: Stack = Stack(),
) {
    data class Region(
        val static: String = "ap-south-1",
        val auto: Boolean = true,
    )

    data class Credentials(
        val accessKey: String? = null,
        val secretKey: String? = null,
        val instanceProfile: Boolean = true,
        val profileName: String? = null,
        val profilePath: String? = null,
    )

    data class S3Properties(
        val region: String? = null,
        val endpoint: String? = null,
        val pathStyleAccessEnabled: Boolean = false,
        val checksumValidationEnabled: Boolean = true,
        val chunkedEncodingDisabled: Boolean = false,
        val accelerateModeEnabled: Boolean = false,
        val dualstackEnabled: Boolean = false,
    )

    data class SnsProperties(
        val region: String? = null,
        val endpoint: String? = null,
    )

    data class Stack(
        val auto: Boolean = false,
        val name: String? = null,
    )
}

/**
 * Application-specific AWS properties that complement Spring Cloud AWS
 */
@ConfigurationProperties(prefix = "ampairs.aws")
data class AmpairsAwsProperties(
    val s3: S3Config = S3Config(),
    val sns: SnsConfig = SnsConfig(),
) {
    data class S3Config(
        val defaultBucket: String = "",
        val uploadFolder: String = "uploads",
        val maxFileSize: String = "10MB",
        val allowedContentTypes: List<String> = listOf(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain", "application/json"
        ),
    )

    data class SnsConfig(
        val enabled: Boolean = true,
        val defaultSenderId: String? = null,
        val smsType: String = "Transactional", // or "Promotional"
    )
}

@Configuration
@EnableConfigurationProperties(SpringCloudAwsProperties::class, AmpairsAwsProperties::class)
class SpringCloudAwsConfig {
    // Spring Cloud AWS will auto-configure S3Template, SnsTemplate, etc.
}