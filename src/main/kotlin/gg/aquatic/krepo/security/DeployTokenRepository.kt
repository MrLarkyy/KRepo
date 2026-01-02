
package gg.aquatic.krepo.security

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeployTokenRepository : JpaRepository<DeployToken, Long> {
    // We search by owner since we can't search by hash (without checking every one)
    fun findAllByOwnerUsername(username: String): List<DeployToken>
    fun findByOwnerUsernameAndName(username: String, name: String): DeployToken?
}
