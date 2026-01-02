package gg.aquatic.krepo.error

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import software.amazon.awssdk.services.s3.model.S3Exception

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(KRepoException::class)
    fun handleKRepoException(ex: KRepoException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(ex.status).body(
            mapOf("error" to ex.code, "message" to ex.message)
        )
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
    fun handleSpringAccessDenied(ex: Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            mapOf("error" to "unauthorized", "message" to (ex.message ?: "Access denied"))
        )
    }

    @ExceptionHandler(StorageException::class)
    fun handleStorageError(ex: StorageException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            mapOf(
                "error" to "storage_error",
                "message" to (ex.message ?: "Unknown storage error")
            )
        )
    }

    @ExceptionHandler(S3Exception::class)
    fun handleS3Error(ex: S3Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            mapOf(
                "error" to "s3_provider_error",
                "message" to (ex.message ?: "S3 communication error"),
                "aws_code" to ex.awsErrorDetails().errorCode()
            )
        )
    }

    // Fallback for unexpected errors
    @ExceptionHandler(Exception::class)
    fun handleGeneralError(ex: Exception): ResponseEntity<Map<String, String>> {
        log.error("Unhandled exception caught: ", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "error" to "internal_server_error",
                "message" to (ex.message ?: "An unexpected error occurred")
            )
        )
    }
}
