package gg.aquatic.krepo.security

import gg.aquatic.krepo.repository.RepositoryEntity
import gg.aquatic.krepo.repository.RepositoryRepository
import gg.aquatic.krepo.repository.RepositoryVisibility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder

    @Autowired
    private lateinit var deployTokenRepository: DeployTokenRepository

    @Autowired
    private lateinit var repositoryRepository: RepositoryRepository

    @BeforeEach
    fun setup() {
        repositoryRepository.deleteAll()
        repositoryRepository.saveAndFlush(RepositoryEntity("private-repo", visibility = RepositoryVisibility.PRIVATE))
        repositoryRepository.saveAndFlush(RepositoryEntity("public-repo", visibility = RepositoryVisibility.PUBLIC))
    }

    @Test
    fun `should allow anonymous access to public repository`() {
        val result = mockMvc.get("/public-repo/any-file.jar").andReturn()

        mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isNotFound) // Correctly waits for the suspend function result
    }

    @Test
    fun `should return 401 for unauthorized request to private repo`() {
        val result = mockMvc.get("/private-repo/some-file.jar").andReturn()

        mockMvc.perform(asyncDispatch(result))
            .andDo { print() }
            .andExpect(status().isUnauthorized) // Standard Java-style matcher

    }

    @Test
    fun `should allow access to private repo with valid JWT`() {
        val token = jwtService.generateToken("admin")

        // Use the actual path that triggers the service logic
        val result = mockMvc.get("/private-repo/test.jar") {
            header("Authorization", "Bearer $token")
        }.andReturn()

        mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isNotFound) // Found the repo, but file doesn't exist
    }

    @Test
    fun `should return 200 for request with valid JWT`() {
        val token = jwtService.generateToken("admin")

        mockMvc.get("/api/test-secure") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }
}

// Dummy controller for the test
@RestController
class TestController {
    @GetMapping("/api/test-secure")
    fun secureEndpoint() = "Success"
}
