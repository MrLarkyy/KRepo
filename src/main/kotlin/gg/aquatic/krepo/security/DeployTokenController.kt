package gg.aquatic.krepo.security

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tokens")
class DeployTokenController(
    private val tokenService: DeployTokenService,
    private val tokenRepository: DeployTokenRepository
) {

    @PostMapping
    fun create(
        @RequestBody request: CreateTokenRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): TokenResponse {
        val (token, rawValue) = tokenService.createToken(userDetails.username, request.name, request.permissions)
        return TokenResponse(token.name, rawValue)
    }

    @GetMapping
    fun list(@AuthenticationPrincipal userDetails: UserDetails): List<TokenSummary> {
        return tokenRepository.findAllByOwnerUsername(userDetails.username).map {
            TokenSummary(it.id!!, it.name, it.createdAt, it.permissions)
        }
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ) {
        val token = tokenRepository.findById(id).orElse(null)
        if (token?.owner?.username == userDetails.username) {
            tokenRepository.delete(token)
        }
    }
}

data class CreateTokenRequest(val name: String, val permissions: Set<String>)
data class TokenResponse(val name: String, val token: String)
data class TokenSummary(val id: Long, val name: String, val createdAt: java.time.Instant, val permissions: Set<String>)
