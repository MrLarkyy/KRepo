package gg.aquatic.krepo.security

import gg.aquatic.krepo.error.AuthenticationFailedException
import gg.aquatic.krepo.error.UserAlreadyExistsException
import gg.aquatic.krepo.user.User
import gg.aquatic.krepo.user.UserRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager
) {
    fun register(request: AuthRequest): String {
        if (userRepository.findByUsername(request.username) != null) {
            throw UserAlreadyExistsException(request.username)
        }

        val user = User(
            username = request.username,
            password = passwordEncoder.encode(request.password)!!
        )
        userRepository.save(user)
        return jwtService.generateToken(user.username)
    }

    fun authenticate(request: AuthRequest): String {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.username, request.password)
            )
        } catch (e: BadCredentialsException) {
            throw AuthenticationFailedException()
        }

        val user = userRepository.findByUsername(request.username)
            ?: throw AuthenticationFailedException("User not found after authentication")

        return jwtService.generateToken(user.username)
    }

    fun logout(token: String) {
        val expiry = jwtService.extractExpiration(token) ?: return
        tokenRepository.save(BlacklistedToken(token, expiry.toInstant()))
    }
}
