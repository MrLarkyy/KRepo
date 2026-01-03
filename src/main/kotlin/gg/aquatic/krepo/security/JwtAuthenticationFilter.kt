package gg.aquatic.krepo.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.core.Authentication
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

        if (authHeader != null) {
            if (authHeader.startsWith("Bearer ")) {
                handleJwtAuth(authHeader.substring(7), request)
            } else if (authHeader.startsWith("Basic ")) {
                val base64Auth = authHeader.substring(6).trim()
                try {
                    val decoded = String(java.util.Base64.getDecoder().decode(base64Auth), Charsets.UTF_8)
                    val parts = decoded.split(":", limit = 2)
                    if (parts.size == 2) {
                        val username = parts[0]
                        val secret = parts[1]

                        if (secret.startsWith("tk_")) {
                            handleDeployTokenAuth(username, secret, request)
                        } else {
                            handleStandardBasicAuth(username, secret, request)
                        }
                    }
                } catch (e: Exception) {
                    // Invalid auth format
                }
            }
        }

        // Always call the chain exactly once at the very end
        filterChain.doFilter(request, response)
    }

    private fun handleDeployTokenAuth(username: String, secret: String, request: HttpServletRequest) {
        val tokens = deployTokenRepository.findAllByOwnerUsername(username)
        val matchedToken = tokens.find { passwordEncoder.matches(secret, it.tokenHash) }

        if (matchedToken != null) {
            val authorities = matchedToken.permissions.map {
                org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TOKEN_${it.uppercase()}")
            }

            val userDetails = userDetailsService.loadUserByUsername(username)
            val authToken = UsernamePasswordAuthenticationToken(userDetails, null, authorities)
            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authToken
        }
    }

    private fun handleStandardBasicAuth(username: String, secret: String, request: HttpServletRequest) {
        try {
            val authToken = UsernamePasswordAuthenticationToken(username, secret)
            authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            val authenticated = authenticationManager.authenticate(authToken)
            SecurityContextHolder.getContext().authentication = authenticated
        } catch (e: Exception) {
            // Auth failed, ignore and let filter chain continue (will result in 401 later)
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