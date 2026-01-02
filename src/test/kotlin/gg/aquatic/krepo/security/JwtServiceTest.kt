package gg.aquatic.krepo.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtServiceTest {

    private lateinit var jwtService: JwtService
    private val secret = "a-very-long-and-secure-secret-key-for-testing-purposes-only"

    @BeforeEach
    fun setUp() {
        val properties = SecurityProperties(secretKey = secret)
        jwtService = JwtService(properties)
    }

    @Test
    fun `should generate and extract username from token`() {
        val username = "testuser"
        val token = jwtService.generateToken(username)

        assertThat(token).isNotBlank()
        assertThat(jwtService.extractUsername(token)).isEqualTo(username)
    }

    @Test
    fun `should return true for valid token`() {
        val token = jwtService.generateToken("admin")
        assertThat(jwtService.isTokenValid(token)).isTrue()
    }

    @Test
    fun `should return false for invalid token`() {
        assertThat(jwtService.isTokenValid("invalid.token.here")).isFalse()
    }
}
