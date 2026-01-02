package gg.aquatic.krepo.repository

import jakarta.persistence.*

@Entity
@Table(name = "repositories")
class RepositoryEntity(
    @Id
    val name: String, // e.g., "releases", "snapshots"

    @Column(nullable = false)
    var hidden: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var visibility: RepositoryVisibility = RepositoryVisibility.PUBLIC
)

enum class RepositoryVisibility {
    PUBLIC,   // Anyone can read
    PRIVATE,  // Only authorized users can read
    HIDDEN    // Not listed, but accessible if you know the path and have perms
}
