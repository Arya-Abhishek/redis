package Cache
import config.ReplicationConfig
import java.util.concurrent.ConcurrentHashMap

class RedisCache {
    private val cache = ConcurrentHashMap<String, CacheValue>()

    fun add(cacheValue: CacheValue) {
        cache[cacheValue.key] = cacheValue
    }

    fun set(key: String, value: String, ttl: Long = -1) {
        cache[key] = CacheValue(key, value, ttl)
    }

    fun get(key: String): CacheValue? {
        val cacheValue = cache[key]
        if (cacheValue != null && (cacheValue.ttl == -1L || cacheValue.ttl > System.currentTimeMillis())) {
            return cacheValue
        }

        cache.remove(key)
        return null
    }

    fun getMatchingKeys(pattern: String): List<String> {
        return cache.keys.toList()
            .filter { it.matches(pattern.replace("*", ".*").toRegex()) }

    }

    fun getInfo(replicationConfig: ReplicationConfig): String {
        val infoRes = """
            # Replication
            role:${replicationConfig.getRole()}
            connected_slaves:0
            master_replid:${replicationConfig.getMasterReplid()}
            master_repl_offset:${replicationConfig.getMasterReplOffset()}
            second_repl_offset:-1
            repl_backlog_active:0
            repl_backlog_size:1048576
            repl_backlog_first_byte_offset:0
            repl_backlog_histlen:0
        """.trimIndent()

        return infoRes
    }
}

data class CacheValue(val key: String, val value: String, val ttl: Long)

