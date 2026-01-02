package gg.aquatic.krepo.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
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

    @Test
    fun `should return 403 for unauthorized request`() {
        mockMvc.get("/api/test-secure").andExpect {
            status { isForbidden() }
        }
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
