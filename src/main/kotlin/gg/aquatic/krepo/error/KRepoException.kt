package gg.aquatic.krepo.error

import org.springframework.http.HttpStatus

/**
 * Base exception for all domain-specific KRepo issues.
 */
open class KRepoException(
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    val code: String = "internal_error"
) : RuntimeException(message)

class UserAlreadyExistsException(username: String) : KRepoException(
    message = "User with username '$username' already exists",
    status = HttpStatus.CONFLICT,
    code = "user_exists"
)

class AuthenticationFailedException(message: String = "Invalid username or password") : KRepoException(
    message = message,
    status = HttpStatus.UNAUTHORIZED,
    code = "auth_failed"
)
class KRepoAccessDeniedException(message: String = "You do not have permission to access this resource") : KRepoException(
    message = message,
    status = HttpStatus.UNAUTHORIZED,
    code = "unauthorized"
)