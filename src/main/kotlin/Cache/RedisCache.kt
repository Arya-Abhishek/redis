package Cache
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
}

data class CacheValue(val key: String, val value: String, val ttl: Long)

