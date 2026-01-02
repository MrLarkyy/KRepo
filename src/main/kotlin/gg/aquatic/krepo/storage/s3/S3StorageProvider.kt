package gg.aquatic.krepo.storage.s3

import gg.aquatic.krepo.error.StorageException
import gg.aquatic.krepo.storage.StorageProvider
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import java.io.InputStream
import java.net.URI
import java.util.concurrent.Executors

class S3StorageProvider(private val properties: S3StorageProperties) : StorageProvider {

    private val log = LoggerFactory.getLogger(S3StorageProvider::class.java)
    private val s3ReadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2)


    private val s3Client: S3AsyncClient = S3AsyncClient.builder()
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
            s3Client.headBucket(headRequest).join()
            log.info("S3 connection verified successfully.")
        } catch (e: Exception) {
            val cause = e.cause ?: e
            throw when (cause) {
                is NoSuchBucketException -> StorageException("S3 Bucket '${properties.bucket}' does not exist!", cause)
                else -> StorageException("Failed to connect to S3: ${cause.message}", cause)
            }
        }
    }

    override suspend fun put(path: String, data: InputStream, contentLength: Long): Result<Unit> = runCatching {
        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket)
                .key(path)
                .contentLength(contentLength)
                .build()

            // Pass the dedicated ExecutorService to the SDK
            val body = AsyncRequestBody.fromInputStream(
                data,
                contentLength,
                s3ReadExecutor
            )

            s3Client.putObject(putObjectRequest, body).await()
        } catch (e: Exception) {
            throw StorageException("Failed to upload $path", e.cause ?: e)
        }
    }

    override suspend fun get(path: String): InputStream? = try {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(properties.bucket)
            .key(path)
            .build()

        // We use the transformer to get a ResponseInputStream
        // Note: The generic type of s3Client.getObject must match the transformer's return type
        s3Client.getObject(getObjectRequest, AsyncResponseTransformer.toBlockingInputStream()).await()
    } catch (e: Exception) {
        val cause = e.cause ?: e
        if (cause is NoSuchKeyException) {
            null
        } else {
            throw StorageException("Error downloading $path", cause)
        }
    }

    override suspend fun delete(path: String): Boolean = try {
        val deleteRequest = DeleteObjectRequest.builder()
            .bucket(properties.bucket)
            .key(path)
            .build()

        s3Client.deleteObject(deleteRequest).await()
        true
    } catch (e: Exception) {
        log.error("Failed to delete $path", e.cause ?: e)
        false
    }

    override suspend fun exists(path: String): Boolean = try {
        val headRequest = HeadObjectRequest.builder()
            .bucket(properties.bucket)
            .key(path)
            .build()

        s3Client.headObject(headRequest).await()
        true
    } catch (e: Exception) {
        val cause = e.cause ?: e
        if (cause is NoSuchKeyException) false else throw StorageException("Error checking $path", cause)
    }

    override suspend fun usage(): Long = try {
        var totalSize = 0L
        val listRequest = ListObjectsV2Request.builder()
            .bucket(properties.bucket)
            .build()

        // Paginator in AsyncClient returns a publisher
        s3Client.listObjectsV2Paginator(listRequest).subscribe { response ->
            totalSize += response.contents().sumOf { it.size() }
        }.await()

        totalSize
    } catch (e: Exception) {
        throw StorageException("Failed to calculate storage usage", e.cause ?: e)
    }

    override fun shutdown() {
        s3ReadExecutor.shutdown()
        s3Client.close()
    }
}