package gg.aquatic.krepo.repository

import gg.aquatic.krepo.error.KRepoAccessDeniedException
import gg.aquatic.krepo.storage.StorageProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*
import kotlin.test.assertEquals

class RepositoryServiceTest {

    private val repositoryRepository = mock(RepositoryRepository::class.java)
    private val storageProvider = mock(StorageProvider::class.java)
    private val authenticationManager = mock(AuthenticationManager::class.java)
    private val service = RepositoryService(repositoryRepository, storageProvider, authenticationManager)

    @Test
    fun `getFile should throw exception when repository is private and user is anonymous`(): Unit = runBlocking {
        val repoName = "private-repo"
        val repo = RepositoryEntity(repoName, visibility = RepositoryVisibility.PRIVATE)

        `when`(repositoryRepository.findById(repoName)).thenReturn(Optional.of(repo))
        SecurityContextHolder.clearContext()

        assertThrows<KRepoAccessDeniedException> {
            service.getFile(repoName, "test.jar")
        }
    }

    @Test
    fun `exists should return false if repo does not exist`() = runBlocking {
        `when`(repositoryRepository.existsById("unknown")).thenReturn(false)

        val result = service.exists("unknown", "any/path")

        assertFalse(result)
    }
}
