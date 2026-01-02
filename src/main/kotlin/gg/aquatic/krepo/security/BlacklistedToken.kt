package gg.aquatic.krepo.security

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "blacklisted_tokens")
class BlacklistedToken(
    @Id
    val token: String,
    
    @Column(nullable = false)
    val expiryDate: Instant
)
