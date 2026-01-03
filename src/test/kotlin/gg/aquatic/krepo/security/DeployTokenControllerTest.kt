package gg.aquatic.krepo.security

import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@AutoConfigureMockMvc
class DeployTokenControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    @Test
    @WithMockUser(username = "admin")
    fun `should create token successfully`() {
        val request = CreateTokenRequest("test-token", setOf("READ", "WRITE"))

        mockMvc.perform(
            post("/api/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("test-token"))
            .andExpect(jsonPath("$.token").value(startsWith("tk_")))
    }

    @Test
    @WithMockUser(username = "user")
    fun `should list tokens for authenticated user`() {
        mockMvc.perform(get("/api/tokens"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    @Test
    fun `should return 401 when listing tokens without auth`() {
        mockMvc.perform(get("/api/tokens"))
            .andExpect(status().isUnauthorized)
    }
}
