package com.ampairs.core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client


private val ACCESS_KEY_ID = "AKIA2N6MZHTD5YV4LNMC"
private val SECRET_ACCESS_KEY = "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"

@Configuration
class AWSConfig {
    fun credentials(): StaticCredentialsProvider {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY_ID, SECRET_ACCESS_KEY))
    }

    @Bean(destroyMethod = "close")
    fun s3Client(): S3Client {
        return S3Client.builder()
            .region(Region.AP_SOUTH_1)
            .credentialsProvider(credentials())
            .build()
    }
}