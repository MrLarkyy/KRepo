package gg.aquatic.krepo.storage.s3

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "krepo.storage.s3")
data class S3StorageProperties(
    val bucket: String,
    val region: String = "us-east-1",
    val accessKey: String,
    val secretKey: String,
    val endpoint: String? = null, // Useful for MinIO or Custom S3 providers
    val pathStyleAccess: Boolean = false // Required for some S3-compatible APIs
)