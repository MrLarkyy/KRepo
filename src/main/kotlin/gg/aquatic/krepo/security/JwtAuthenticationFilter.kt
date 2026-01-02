package gg.aquatic.krepo.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: UserDetailsService,
    private val tokenRepository: TokenRepository,
    private val deployTokenRepository: DeployTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val applicationContext: ApplicationContext
) : OncePerRequestFilter() {

    // Lazy access to the manager to break the circular dependency
    private val authenticationManager: AuthenticationManager by lazy {
        applicationContext.getBean<AuthenticationManager>()
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null) {
            filterChain.doFilter(request, response)
            return
        }

        if (authHeader.startsWith("Bearer ")) {
            handleJwtAuth(authHeader.substring(7), request)
        } else if (authHeader.startsWith("Basic ")) {
            handleBasicAuth(authHeader.substring(6), request)
        }

        filterChain.doFilter(request, response)
    }

    private fun handleBasicAuth(base64Auth: String, request: HttpServletRequest) {
        try {
            val decoded = String(java.util.Base64.getDecoder().decode(base64Auth))
            val parts = decoded.split(":", limit = 2)
            if (parts.size != 2) return

            val username = parts[0]
            val secret = parts[1]

            // 1. Check if it's a Deploy Token
            val tokens = deployTokenRepository.findAllByOwnerUsername(username)
            val matchedToken = tokens.find { passwordEncoder.matches(secret, it.tokenHash) }

            if (matchedToken != null) {
                // Map permissions to Authorities (e.g., READ -> ROLE_TOKEN_READ)
                val authorities = matchedToken.permissions.map {
                    org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TOKEN_$it")
                }

                val userDetails = userDetailsService.loadUserByUsername(username)
                val authToken = UsernamePasswordAuthenticationToken(userDetails, null, authorities)
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
                return
            }

            // 2. Fallback to standard password auth
            val authToken = UsernamePasswordAuthenticationToken(username, secret)
            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            val authenticated = authenticationManager.authenticate(authToken)
            SecurityContextHolder.getContext().authentication = authenticated

        } catch (e: Exception) {
            // Ignore failed auth
        }
    }

    private fun handleJwtAuth(jwt: String, request: HttpServletRequest) {
        if (tokenRepository.existsByToken(jwt)) return

        val username = jwtService.extractUsername(jwt)
        if (username != null && SecurityContextHolder.getContext().authentication == null) {
            val userDetails = userDetailsService.loadUserByUsername(username)
            if (jwtService.isTokenValid(jwt)) {
                val authToken = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }
    }
}