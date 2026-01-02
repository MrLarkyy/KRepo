package gg.aquatic.krepo.security

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface TokenRepository : JpaRepository<BlacklistedToken, String> {
    fun existsByToken(token: String): Boolean
    fun deleteByExpiryDateBefore(now: Instant)
}
