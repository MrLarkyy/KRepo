package gg.aquatic.krepo.error

/**
 * Base exception for all storage-related issues.
 * Wrapping provider-specific exceptions (like S3Exception) into this
 * prevents infrastructure leaks into the higher layers.
 */
open class StorageException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
