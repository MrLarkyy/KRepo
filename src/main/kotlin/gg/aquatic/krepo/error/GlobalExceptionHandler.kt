package gg.aquatic.krepo.error

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import software.amazon.awssdk.services.s3.model.S3Exception

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(StorageException::class)
    fun handleStorageError(ex: StorageException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            mapOf(
                "error" to "Storage Provider Error",
                "message" to (ex.message ?: "Unknown error occurred")
            )
        )
    }

    @ExceptionHandler(S3Exception::class)
    fun handleS3Error(ex: S3Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            mapOf(
                "error" to "Storage Provider Error",
                "message" to (ex.message ?: "Unknown error occurred while communicating with S3"),
                "code" to ex.awsErrorDetails().errorCode()
            )
        )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf("error" to "Internal Configuration Error", "message" to (ex.message ?: "Check server logs"))
        )
    }
}
