package gg.aquatic.krepo.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RepositoryRepository : JpaRepository<RepositoryEntity, String>
