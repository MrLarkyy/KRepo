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

        val rawToken = generateRawToken()

        val token = DeployToken(
            name = name,
            tokenHash = passwordEncoder.encode(rawToken)!!,
            owner = user,
            permissions = permissions
        )

        return deployTokenRepository.save(token) to rawToken
    }

    fun resetToken(username: String, tokenName: String): String {
        val token = deployTokenRepository.findByOwnerUsernameAndName(username, tokenName)
            ?: throw IllegalArgumentException("Token '$tokenName' for user '$username' not found")

        val newRawValue = generateRawToken()
        token.tokenHash = passwordEncoder.encode(newRawValue)!!
        deployTokenRepository.save(token)

        return newRawValue
    }

    private fun generateRawToken(): String = "tk_" + UUID.randomUUID().toString().replace("-", "")
}
