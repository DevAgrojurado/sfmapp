package com.agrojurado.sfmappv2.domain.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

class CacheManager<V> {
    private data class CacheEntry<V>(
        val data: V,
        val timestamp: Long
    )

    private val cache = mutableMapOf<String, CacheEntry<V>>()
    private val mutex = Mutex()
    private val defaultExpirationTime = TimeUnit.MINUTES.toMillis(30) // 30 minutos por defecto

    suspend fun get(key: String): V? {
        return mutex.withLock {
            val entry = cache[key]
            if (entry != null && !isExpired(entry.timestamp)) {
                entry.data
            } else {
                cache.remove(key)
                null
            }
        }
    }

    suspend fun set(key: String, value: V) {
        mutex.withLock {
            cache[key] = CacheEntry(value, System.currentTimeMillis())
        }
    }

    suspend fun getOrFetch(
        key: String,
        expirationTime: Long = defaultExpirationTime,
        fetch: suspend () -> V
    ): V {
        val cachedValue = get(key)
        return if (cachedValue != null) {
            cachedValue
        } else {
            fetch().also { set(key, it) }
        }
    }

    suspend fun getOrFetchFlow(
        key: String,
        fetch: suspend () -> Flow<V>
    ): V {
        return getOrFetch(key) { fetch().first() }
    }

    suspend fun invalidate(key: String) {
        mutex.withLock {
            cache.remove(key)
        }
    }

    suspend fun invalidateAll() {
        mutex.withLock {
            cache.clear()
        }
    }

    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > defaultExpirationTime
    }

    companion object {
        private val instances = mutableMapOf<String, CacheManager<*>>()
        private val instanceMutex = Mutex()

        suspend fun <V> getInstance(name: String): CacheManager<V> {
            return instanceMutex.withLock {
                @Suppress("UNCHECKED_CAST")
                instances.getOrPut(name) { CacheManager<V>() } as CacheManager<V>
            }
        }
    }
}