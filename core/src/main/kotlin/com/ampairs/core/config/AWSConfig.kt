package com.ampairs.core.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sns.SnsClient

@ConfigurationProperties(prefix = "aws")
data class AwsProperties(
    val region: String = "ap-south-1",
    val s3: S3Properties = S3Properties(),
    val sns: SnsProperties = SnsProperties(),
    val credentials: CredentialsProperties = CredentialsProperties(),
) {
    data class S3Properties(
        val bucket: String = "",
        val endpoint: String? = null,
    )

    data class SnsProperties(
        val enabled: Boolean = true,
    )

    data class CredentialsProperties(
        val accessKey: String? = null,
        val secretKey: String? = null,
        val useIamRole: Boolean = true,
    )
}

@Configuration
@EnableConfigurationProperties(AwsProperties::class)
class AWSConfig(private val awsProperties: AwsProperties) {

    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider {
        return if (awsProperties.credentials.useIamRole ||
            awsProperties.credentials.accessKey.isNullOrBlank()
        ) {
            DefaultCredentialsProvider.create()
        } else {
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    awsProperties.credentials.accessKey!!,
                    awsProperties.credentials.secretKey!!
                )
            )
        }
    }

    @Bean(destroyMethod = "close")
    fun s3Client(credentialsProvider: AwsCredentialsProvider): S3Client {
        val builder = S3Client.builder()
            .region(Region.of(awsProperties.region))
            .credentialsProvider(credentialsProvider)

        awsProperties.s3.endpoint?.let { builder.endpointOverride(java.net.URI.create(it)) }

        return builder.build()
    }

    @Bean(destroyMethod = "close")
    @Profile("!test")
    fun snsClient(credentialsProvider: AwsCredentialsProvider): SnsClient {
        return SnsClient.builder()
            .region(Region.of(awsProperties.region))
            .credentialsProvider(credentialsProvider)
            .build()
    }
}