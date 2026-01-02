package gg.aquatic.krepo.storage

import java.io.InputStream

interface StorageProvider {
    suspend fun put(path: String, data: InputStream, contentLength: Long): Result<Unit>
    suspend fun get(path: String): InputStream?
    suspend fun delete(path: String): Boolean
    suspend fun exists(path: String): Boolean
    suspend fun usage(): Long

    fun shutdown()
}