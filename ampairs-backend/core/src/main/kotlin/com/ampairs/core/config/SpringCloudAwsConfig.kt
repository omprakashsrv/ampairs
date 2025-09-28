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

/**
 * Object Storage configuration properties supporting both S3 and MinIO
 */
@ConfigurationProperties(prefix = "ampairs.storage")
data class StorageProperties(
    val provider: StorageProvider = StorageProvider.LOCAL,
    val defaultBucket: String = "ampairs-files",
    val maxFileSize: String = "10MB",
    val allowedContentTypes: List<String> = listOf(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/tiff"
    ),
    val image: ImageConfig = ImageConfig(),
    val local: LocalConfig = LocalConfig(),
) {
    data class ImageConfig(
        val maxWidth: Int = 2048,
        val maxHeight: Int = 2048,
        val quality: Int = 85,
        val cacheMaxAge: Long = 86400, // 24 hours
        val thumbnails: ThumbnailConfig = ThumbnailConfig(),
    )

    data class ThumbnailConfig(
        val enabled: Boolean = true,
        val cacheEnabled: Boolean = true,
        val cacheMaxAge: Long = 604800, // 7 days
        val supportedSizes: List<Int> = listOf(150, 300, 500),
        val defaultSize: Int = 300,
        val format: String = "jpg",
        val autoGenerate: Boolean = false, // Generate on upload vs on-demand
    )

    data class LocalConfig(
        val enabled: Boolean = true,
        val basePath: String = "./storage",
        val createDirectories: Boolean = true,
        val urlBase: String = "http://localhost:8080/files",
    )

    enum class StorageProvider {
        LOCAL, S3, MINIO
    }
}

/**
 * MinIO configuration properties
 */
@ConfigurationProperties(prefix = "ampairs.minio")
data class MinioProperties(
    val enabled: Boolean = false,
    val endpoint: String = "http://localhost:9000",
    val accessKey: String = "minioadmin",
    val secretKey: String = "minioadmin",
    val defaultBucket: String = "ampairs-files",
    val autoCreateBucket: Boolean = true,
    val connectionTimeout: java.time.Duration = java.time.Duration.ofSeconds(10),
    val readTimeout: java.time.Duration = java.time.Duration.ofSeconds(30),
)

@Configuration
@EnableConfigurationProperties(
    SpringCloudAwsProperties::class,
    AmpairsAwsProperties::class,
    StorageProperties::class,
    MinioProperties::class
)
class SpringCloudAwsConfig {
    // Spring Cloud AWS will auto-configure S3Template, SnsTemplate, etc.
}