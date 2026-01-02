package gg.aquatic.krepo.security

import gg.aquatic.krepo.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class DeployTokenService(
    private val deployTokenRepository: DeployTokenRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun createToken(username: String, name: String, permissions: Set<String>): Pair<DeployToken, String> {
        val user = userRepository.findByUsername(username) ?: throw IllegalArgumentException("User not found")

        // Generate a secure random string
        val rawToken = "tk_" + UUID.randomUUID().toString().replace("-", "")

        val token = DeployToken(
            name = name,
            tokenHash = passwordEncoder.encode(rawToken)!!, // Added the non-null assertion
            owner = user,
            permissions = permissions
        )

        return deployTokenRepository.save(token) to rawToken
    }
}
