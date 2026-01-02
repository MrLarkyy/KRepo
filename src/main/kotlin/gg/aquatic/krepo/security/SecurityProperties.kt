package gg.aquatic.krepo.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "krepo.security.jwt")
data class SecurityProperties(
    val secretKey: String,
    val expirationMs: Long = 3600000, // 1 hour
    val refreshTokenExpirationMs: Long = 604800000 // 7 days
)
