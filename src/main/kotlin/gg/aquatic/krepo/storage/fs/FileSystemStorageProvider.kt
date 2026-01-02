package gg.aquatic.krepo.storage.fs

import gg.aquatic.krepo.error.StorageException
import gg.aquatic.krepo.storage.StorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class FileSystemStorageProvider(private val rootPath: String) : StorageProvider {

    private val root = Paths.get(rootPath).toAbsolutePath().normalize()

    init {
        Files.createDirectories(root)
    }

    override suspend fun put(path: String, data: InputStream, contentLength: Long): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val target = root.resolve(path).normalize()
            if (!target.startsWith(root)) throw StorageException("Invalid path: $path")
            
            Files.createDirectories(target.parent)
            data.use { input ->
                Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    override suspend fun get(path: String): InputStream? = withContext(Dispatchers.IO) {
        val target = root.resolve(path).normalize()
        if (target.exists() && target.startsWith(root)) {
            Files.newInputStream(target)
        } else null
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        val target = root.resolve(path).normalize()
        if (target.startsWith(root)) Files.deleteIfExists(target) else false
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        val target = root.resolve(path).normalize()
        target.exists() && target.startsWith(root)
    }

    override suspend fun usage(): Long = withContext(Dispatchers.IO) {
        Files.walk(root).filter { Files.isRegularFile(it) }.mapToLong { Files.size(it) }.sum()
    }

    override fun shutdown() {}
}
