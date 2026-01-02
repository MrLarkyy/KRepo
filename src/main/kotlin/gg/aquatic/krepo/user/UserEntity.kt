package gg.aquatic.krepo.user

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val username: String = "", // Add default value

    @Column(nullable = false)
    var password: String = "", // Add default value

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    @Column(name = "role")
    var roles: Set<String> = setOf("ROLE_USER")
) {
    fun isAdmin(): Boolean = roles.contains("ROLE_ADMIN")
}