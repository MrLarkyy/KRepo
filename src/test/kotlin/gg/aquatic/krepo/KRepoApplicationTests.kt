package gg.aquatic.krepo

import gg.aquatic.krepo.storage.StorageProvider
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@AutoConfigureMockMvc
class KRepoApplicationTests {


    @MockitoBean
    private lateinit var storageProvider: StorageProvider

    @Test
    fun contextLoads() {
    }

}
