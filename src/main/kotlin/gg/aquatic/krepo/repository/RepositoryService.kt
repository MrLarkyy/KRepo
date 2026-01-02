package gg.aquatic.krepo.repository

import gg.aquatic.krepo.error.KRepoAccessDeniedException
import gg.aquatic.krepo.error.KRepoException
import gg.aquatic.krepo.storage.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.io.InputStream
import kotlin.jvm.optionals.getOrNull

@Service
class RepositoryService(
    private val repositoryRepository: RepositoryRepository,
    private val storageProvider: StorageProvider
) {
    suspend fun getFile(repositoryName: String, path: String): InputStream {
        val context = SecurityContextHolder.getContext()
        return withContext(Dispatchers.IO) {
            // Manually re-set context for the IO thread if asContextElement() isn't available
            val originalContext = SecurityContextHolder.getContext()
            try {
                SecurityContextHolder.setContext(context)

                val repo = repositoryRepository.findById(repositoryName).getOrNull()
                    ?: throw KRepoException("Repository '$repositoryName' not found", HttpStatus.NOT_FOUND)

                if (repo.visibility == RepositoryVisibility.PRIVATE) {
                    val auth = SecurityContextHolder.getContext().authentication
                    if (auth == null || !auth.isAuthenticated || auth.principal == "anonymousUser") {
                        throw KRepoAccessDeniedException("Authentication required for private repository '$repositoryName'")
                    }
                }

                val storagePath = "$repositoryName/${path.trimStart('/')}"
                storageProvider.get(storagePath)
                    ?: throw KRepoException("File not found: $path", HttpStatus.NOT_FOUND)
            } finally {
                SecurityContextHolder.setContext(originalContext)
            }
        }
    }

    suspend fun exists(repositoryName: String, path: String): Boolean = withContext(Dispatchers.IO) {
        if (!repositoryRepository.existsById(repositoryName)) {
            return@withContext false
        }

        val storagePath = "$repositoryName/${path.trimStart('/')}"
        return@withContext storageProvider.exists(storagePath)
    }

    suspend fun uploadFile(
        repositoryName: String,
        path: String,
        inputStream: InputStream,
        contentLength: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!repositoryRepository.existsById(repositoryName)) {
            repositoryRepository.save(RepositoryEntity(repositoryName))
        }

        val storagePath = "$repositoryName/${path.trimStart('/')}"
        return@withContext storageProvider.put(storagePath, inputStream, contentLength)
    }
}
