package gg.aquatic.krepo.repository

import gg.aquatic.krepo.error.KRepoAccessDeniedException
import gg.aquatic.krepo.error.KRepoException
import gg.aquatic.krepo.security.DeployTokenRepository
import gg.aquatic.krepo.storage.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.Base64
import kotlin.jvm.optionals.getOrNull

@Service
class RepositoryService(
    private val repositoryRepository: RepositoryRepository,
    private val storageProvider: StorageProvider,
    private val deployTokenRepository: DeployTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager
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
        contentLength: Long,
        authHeader: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            validateUploadAuth(authHeader)

            if (path.startsWith("/") || path.contains("..")) {
                throw KRepoException("Invalid upload path", HttpStatus.BAD_REQUEST)
            }

            if (!repositoryRepository.existsById(repositoryName)) {
                repositoryRepository.save(RepositoryEntity(repositoryName))
            }

            val storagePath = "$repositoryName/${path.trimStart('/')}"
            storageProvider.put(storagePath, inputStream, contentLength)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateUploadAuth(authHeader: String) {
        val base64Auth = authHeader.removePrefix("Basic ").trim()
        val decoded = String(Base64.getDecoder().decode(base64Auth))
        val parts = decoded.split(":", limit = 2)
        if (parts.size != 2) throw KRepoAccessDeniedException("Invalid Authorization header format")

        val username = parts[0]
        val secret = parts[1]

        if (secret.startsWith("tk_")) {
            // Validate Deploy Token
            val tokens = deployTokenRepository.findAllByOwnerUsername(username)
            val matchedToken = tokens.find { passwordEncoder.matches(secret, it.tokenHash) }

            if (matchedToken == null || !matchedToken.permissions.contains("WRITE")) {
                throw KRepoAccessDeniedException("Invalid token or missing WRITE permission")
            }
        } else {
            // Validate standard Password
            try {
                val auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, secret))
                val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
                if (!isAdmin) throw KRepoAccessDeniedException("Admin role required for password-based upload")
            } catch (e: Exception) {
                throw KRepoAccessDeniedException("Invalid credentials")
            }
        }
    }
}
