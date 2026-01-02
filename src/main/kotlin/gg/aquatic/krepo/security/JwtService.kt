package gg.aquatic.krepo.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(private val properties: SecurityProperties) {

    private val signingKey: SecretKey = Keys.hmacShaKeyFor(properties.secretKey.toByteArray())

    fun generateToken(username: String, extraClaims: Map<String, Any> = emptyMap()): String {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + properties.expirationMs))
            .signWith(signingKey)
            .compact()
    }

    fun extractUsername(token: String): String? {
        return runCatching {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
        }.getOrNull()
    }

    fun isTokenValid(token: String): Boolean {
        return extractUsername(token) != null
    }

    fun extractExpiration(token: String): Date? {
        return runCatching {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .expiration
        }.getOrNull()
    }
}
