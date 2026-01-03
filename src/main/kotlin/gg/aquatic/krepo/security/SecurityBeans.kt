package gg.aquatic.krepo.security

import gg.aquatic.krepo.user.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class SecurityBeans(private val userRepository: UserRepository) {

    @Bean
    fun userDetailsService(): UserDetailsService = UserDetailsService { username ->
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found")

        org.springframework.security.core.userdetails.User
            .withUsername(user.username)
            .password(user.password)
            .authorities(*user.roles.toTypedArray())
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        val authProvider = DaoAuthenticationProvider(userDetailsService())
        authProvider.setPasswordEncoder(passwordEncoder())
        return authProvider
    }

    @Bean
    fun deployTokenAuthenticationProvider(
        deployTokenRepository: DeployTokenRepository,
        userDetailsService: UserDetailsService,
        passwordEncoder: PasswordEncoder
    ): AuthenticationProvider = object : AuthenticationProvider {
        override fun authenticate(authentication: org.springframework.security.core.Authentication): org.springframework.security.core.Authentication? {
            val username = authentication.name
            val password = authentication.credentials.toString()

            if (!password.startsWith("tk_")) return null

            val tokens = deployTokenRepository.findAllByOwnerUsername(username)
            val matchedToken = tokens.find { passwordEncoder.matches(password, it.tokenHash) } ?: return null

            val authorities = matchedToken.permissions.map {
                org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_TOKEN_${it.uppercase()}")
            }
            val userDetails = userDetailsService.loadUserByUsername(username)
            return UsernamePasswordAuthenticationToken(userDetails, null, authorities)
        }

        override fun supports(authentication: Class<*>): Boolean =
            UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager
}
