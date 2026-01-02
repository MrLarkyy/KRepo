package gg.aquatic.krepo.storage.s3

import gg.aquatic.krepo.storage.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.InputStream
import java.net.URI

class S3StorageProvider(private val properties: S3StorageProperties) : StorageProvider {

    private val log = LoggerFactory.getLogger(S3StorageProvider::class.java)

    private val s3Client: S3Client = S3Client.builder()
        .region(Region.of(properties.region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey, properties.secretKey)
            )
        )
        .apply {
            properties.endpoint?.let { endpointOverride(URI.create(it)) }
            if (properties.pathStyleAccess) {
                serviceConfiguration { it.pathStyleAccessEnabled(true) }
            }
        }
        .build()

    /**
     * Verifies that the bucket exists and is accessible.
     * Throws an exception if the configuration is invalid.
     */
    fun verifyConnection() {
        try {
            log.info("Verifying S3 connection to bucket: ${properties.bucket}...")
            val headRequest = HeadBucketRequest.builder().bucket(properties.bucket).build()
            s3Client.headBucket(headRequest)
            log.info("S3 connection verified successfully.")
        } catch (e: NoSuchBucketException) {
            throw IllegalStateException("S3 Bucket '${properties.bucket}' does not exist!")
        } catch (e: S3Exception) {
            throw IllegalStateException("Failed to connect to S3: ${e.message}. Check your credentials and region.", e)
        }
    }

    override suspend fun put(path: String, data: InputStream, contentLength: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket)
                .key(path)
                .contentLength(contentLength)
                .build()

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(data, contentLength))
            Unit
        }
    }

    override suspend fun get(path: String): InputStream? = withContext(Dispatchers.IO) {
        try {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket)
                .key(path)
                .build()

            s3Client.getObject(getObjectRequest)
        } catch (e: NoSuchKeyException) {
            null
        }
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleteRequest = DeleteObjectRequest.builder()
                .bucket(properties.bucket)
                .key(path)
                .build()

            s3Client.deleteObject(deleteRequest)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val headRequest = HeadObjectRequest.builder()
                .bucket(properties.bucket)
                .key(path)
                .build()

            s3Client.headObject(headRequest)
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }

    override suspend fun usage(): Long = withContext(Dispatchers.IO) {
        var totalSize = 0L
        val listRequest = ListObjectsV2Request.builder()
            .bucket(properties.bucket)
            .build()

        s3Client.listObjectsV2Paginator(listRequest).forEach { response ->
            totalSize += response.contents().sumOf { it.size() }
        }
        totalSize
    }
}