package gg.aquatic.krepo.security

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthenticationService) {

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')") // Only admins can register new users
    fun register(@RequestBody request: AuthRequest): AuthResponse {
        return AuthResponse(authService.register(request))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): AuthResponse {
        return AuthResponse(authService.authenticate(request))
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") authHeader: String) {
        val token = authHeader.substring(7)
        authService.logout(token)
    }
}

data class AuthRequest(
    val username: String,
    val password: String
)

data class AuthResponse(val token: String)
