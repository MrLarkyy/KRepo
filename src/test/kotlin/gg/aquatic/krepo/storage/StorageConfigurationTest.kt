package gg.aquatic.krepo.storage

import gg.aquatic.krepo.storage.s3.S3StorageProvider
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class StorageConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(StorageConfiguration::class.java))

    @Test
    fun `should load S3StorageProvider when type is s3`() {
        contextRunner
            .withPropertyValues(
                "krepo.storage.type=s3",
                "krepo.storage.s3.bucket=test-bucket",
                "krepo.storage.s3.access-key=test-access",
                "krepo.storage.s3.secret-key=test-secret"
            )
            .run { context ->
                Assertions.assertThat(context).hasSingleBean(StorageProvider::class.java)
                Assertions.assertThat(context.getBean(StorageProvider::class.java)).isInstanceOf(S3StorageProvider::class.java)
            }
    }

    @Test
    fun `should not load any StorageProvider when type is missing`() {
        contextRunner.run { context ->
            Assertions.assertThat(context).doesNotHaveBean(StorageProvider::class.java)
        }
    }
}