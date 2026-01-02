package gg.aquatic.krepo

import gg.aquatic.krepo.repository.RepositoryRepository
import gg.aquatic.krepo.repository.RepositoryVisibility
import gg.aquatic.krepo.security.DeployTokenService
import gg.aquatic.krepo.user.User
import gg.aquatic.krepo.user.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

@Component
class ConsoleCommands(
    private val repositoryRepository: RepositoryRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val deployTokenService: DeployTokenService
) : CommandLineRunner {

    override fun run(vararg args: String) {
        // Run in a separate thread so it doesn't block app startup
        thread(isDaemon = true, name = "KRepo-Console") {
            val scanner = Scanner(System.`in`)
            println("KRepo Console started. Type 'help' for commands.")

            while (scanner.hasNextLine()) {
                val input = scanner.nextLine().trim()
                if (input.isEmpty()) continue

                val parts = input.split(" ")
                val command = parts[0].lowercase()

                try {
                    when (command) {
                        "help" -> printHelp()
                        "repo-list" -> listRepos()
                        "repo-set-visibility" -> setVisibility(parts)
                        "user-list" -> listUsers()
                        "user-create" -> createUser(parts)
                        "user-delete" -> deleteUser(parts)
                        "user-promote" -> toggleAdmin(parts, true)
                        "user-demote" -> toggleAdmin(parts, false)
                        "token-generate" -> generateToken(parts)
                        "token-reset" -> resetToken(parts)
                        "exit" -> exitProcess(0)
                        else -> println("Unknown command: $command. Type 'help' for info.")
                    }
                } catch (e: Exception) {
                    println("Error executing command: ${e.message}")
                }
                print("> ")
            }
        }
    }

    private fun printHelp() {
        println("""
            Available Commands:
            - repo-list: List all repositories
            - repo-set-visibility <name> <PUBLIC|PRIVATE|HIDDEN>: Change visibility
            - user-list: List all registered users
            - user-create <username> <password> <ROLES...>: Create a user (e.g. ROLE_USER,ROLE_ADMIN)
            - user-delete <username>: Remove a user account
            - user-promote <username>: Add admin role to a user
            - user-demote <username>: Remove admin role from a user
            - token-generate <username> <token_name> <PERMS...>: Generate a deploy token (e.g. READ,WRITE)
            - token-reset <username> <token_name>: Reset an existing token's value
            - exit: Shutdown the application
        """.trimIndent())
    }

    private fun generateToken(parts: List<String>) {
        if (parts.size < 4) return println("Usage: token-generate <username> <token_name> <PERMS...>")
        val username = parts[1]
        val tokenName = parts[2]
        val permissions = parts.drop(3).map { it.uppercase() }.toSet()

        try {
            val (token, rawValue) = deployTokenService.createToken(username, tokenName, permissions)
            println("Successfully generated token '$tokenName' for user '$username'!")
            println("Token: $rawValue")
            println("IMPORTANT: This is the ONLY time you will see this token. Store it securely.")
        } catch (e: Exception) {
            println("Error generating token: ${e.message}")
        }
    }

    private fun resetToken(parts: List<String>) {
        if (parts.size < 3) return println("Usage: token-reset <username> <token_name>")
        val username = parts[1]
        val tokenName = parts[2]

        try {
            val newRawValue = deployTokenService.resetToken(username, tokenName)
            println("Successfully reset token '$tokenName' for user '$username'!")
            println("New Token: $newRawValue")
            println("IMPORTANT: Update your CI/CD scripts with this new value.")
        } catch (e: Exception) {
            println("Error resetting token: ${e.message}")
        }
    }

    private fun listUsers() {
        val users = userRepository.findAll()
        if (users.isEmpty()) return println("No users found.")
        println("Registered Users:")
        users.forEach { println("- ${it.username} [Roles: ${it.roles.joinToString(", ")}]") }
    }

    private fun deleteUser(parts: List<String>) {
        if (parts.size < 2) return println("Usage: user-delete <username>")
        val username = parts[1]
        val user = userRepository.findByUsername(username)
            ?: return println("User '$username' not found.")

        userRepository.delete(user)
        println("User '$username' has been deleted.")
    }

    private fun toggleAdmin(parts: List<String>, promote: Boolean) {
        if (parts.size < 2) return println("Usage: user-${if (promote) "promote" else "demote"} <username>")
        val username = parts[1]
        val user = userRepository.findByUsername(username)
            ?: return println("User '$username' not found.")

        val newRoles = user.roles.toMutableSet()
        if (promote) {
            newRoles.add("ROLE_ADMIN")
        } else {
            newRoles.remove("ROLE_ADMIN")
        }

        user.roles = newRoles
        userRepository.save(user)
        println("User '$username' has been ${if (promote) "promoted to admin" else "demoted to regular user"}.")
    }

    private fun listRepos() {
        val repos = repositoryRepository.findAll()
        if (repos.isEmpty()) return println("No repositories found.")
        repos.forEach { println("- ${it.name} [Visibility: ${it.visibility}]") }
    }

    private fun setVisibility(parts: List<String>) {
        if (parts.size < 3) return println("Usage: repo-set-visibility <name> <visibility>")
        val name = parts[1]
        val visibility = RepositoryVisibility.valueOf(parts[2].uppercase())
        
        val repo = repositoryRepository.findById(name).orElse(null)
            ?: return println("Repository '$name' not found.")
            
        repo.visibility = visibility
        repositoryRepository.save(repo)
        println("Updated '$name' to $visibility.")
    }

    private fun createUser(parts: List<String>) {
        if (parts.size < 3) return println("Usage: user-create <username> <password> <ROLES...>")
        val username = parts[1]
        val password = parts[2]
        // If no roles provided, default to ROLE_USER
        val roles = if (parts.size > 3) parts.drop(3).toSet() else setOf("ROLE_USER")

        if (userRepository.findByUsername(username) != null) {
            return println("User '$username' already exists.")
        }

        val user = User(
            username = username,
            password = passwordEncoder.encode(password)!!,
            roles = roles
        )
        userRepository.save(user)
        println("User '$username' created with roles: ${roles.joinToString(", ")}")
    }
}
