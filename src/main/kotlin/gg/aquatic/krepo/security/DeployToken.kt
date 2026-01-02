package gg.aquatic.krepo.security

import gg.aquatic.krepo.user.User
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "deploy_tokens")
class DeployToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Column(unique = true, nullable = false)
    val tokenHash: String, // Store the HASHED value

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val owner: User,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deploy_token_permissions", joinColumns = [JoinColumn(name = "token_id")])
    @Column(name = "permission")
    val permissions: Set<String> = setOf("READ", "WRITE")
)
