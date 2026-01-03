package gg.aquatic.krepo.repository

import gg.aquatic.krepo.error.KRepoException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaTypeFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.HandlerMapping

@RestController
class ResourceController(private val repositoryService: RepositoryService) {

    @GetMapping("/{repository}/**")
    suspend fun getResource(
        @PathVariable repository: String,
        request: HttpServletRequest
    ): ResponseEntity<InputStreamResource> {
        val path = extractPath(request, repository)
        val inputStream = repositoryService.getFile(repository, path)

        val mediaType = MediaTypeFactory.getMediaType(path)
            .orElse(MediaType.APPLICATION_OCTET_STREAM)

        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(InputStreamResource(inputStream))
    }

    @PutMapping("/{repository}/**")
    suspend fun putResource(
        @PathVariable repository: String,
        @RequestHeader(value = "Content-Length", required = false) contentLength: Long?,
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        request: HttpServletRequest
    ): ResponseEntity<Unit> {
        if (authHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header("WWW-Authenticate", "Basic realm=\"KRepo\"")
                .build()
        }

        val path = extractPath(request, repository)

        val length = contentLength ?: throw KRepoException("Content-Length is required", HttpStatus.LENGTH_REQUIRED)
        if (length <= 0) throw KRepoException("Content-Length must be greater than 0")

        repositoryService.uploadFile(repository, path, request.inputStream, length, authHeader)
            .getOrThrow()

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @RequestMapping(value = ["/{repository}/**"], method = [RequestMethod.HEAD])
    suspend fun headResource(
        @PathVariable repository: String,
        request: HttpServletRequest
    ): ResponseEntity<Unit> {
        val path = extractPath(request, repository)
        val exists = repositoryService.exists(repository, path)
        return if (exists) ResponseEntity.ok().build() else ResponseEntity.notFound().build()
    }

    private fun extractPath(request: HttpServletRequest, repository: String): String {
        val fullPath = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        val path = fullPath.removePrefix("/$repository").trimStart('/')

        if (path.contains("..") || path.contains("//")) {
            throw KRepoException("Invalid path: Path traversal or empty segments are not allowed", HttpStatus.BAD_REQUEST)
        }

        return path
    }
}
