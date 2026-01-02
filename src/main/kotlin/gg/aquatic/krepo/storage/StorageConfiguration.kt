package gg.aquatic.krepo.storage

import gg.aquatic.krepo.storage.fs.FileSystemStorageProvider
import gg.aquatic.krepo.storage.s3.S3StorageProperties
import gg.aquatic.krepo.storage.s3.S3StorageProvider
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StorageConfiguration {

    @Configuration
    @ConditionalOnProperty(name = ["krepo.storage.type"], havingValue = "fs")
    class FileSystemConfiguration {
        @Value($$"${krepo.storage.fs.path:./storage}")
        private lateinit var path: String

        @Bean
        fun fileSystemStorageProvider(): StorageProvider = FileSystemStorageProvider(path)
    }

    @Configuration
    @ConditionalOnProperty(name = ["krepo.storage.type"], havingValue = "s3")
    @EnableConfigurationProperties(S3StorageProperties::class)
    class S3Configuration(private val s3Properties: S3StorageProperties) {

        private var provider: S3StorageProvider? = null

        @Bean(destroyMethod = "shutdown")
        fun s3StorageProvider(): StorageProvider {
            return S3StorageProvider(s3Properties).also { this.provider = it }
        }

        @PostConstruct
        fun validate() {
            provider?.verifyConnection()
        }
    }
}