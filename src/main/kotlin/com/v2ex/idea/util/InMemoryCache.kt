package com.v2ex.idea.util

class InMemoryCache<K, V>(private val maxSize: Int = 256) {
    private data class Entry<V>(val value: V, val expiresAtMs: Long)

    private val map = object : LinkedHashMap<K, Entry<V>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Entry<V>>?): Boolean = size > maxSize
    }

    @Synchronized
    fun get(key: K, nowMs: Long = System.currentTimeMillis()): V? {
        val entry = map[key] ?: return null
        return if (entry.expiresAtMs >= nowMs) {
            entry.value
        } else {
            map.remove(key)
            null
        }
    }

    @Synchronized
    fun put(key: K, value: V, ttlMs: Long, nowMs: Long = System.currentTimeMillis()) {
        map[key] = Entry(value = value, expiresAtMs = nowMs + ttlMs)
    }

    @Synchronized
    fun invalidateByPrefix(prefix: String) {
        val keys = map.keys.filter { it.toString().startsWith(prefix) }
        keys.forEach { map.remove(it) }
    }

    @Synchronized
    fun clear() {
        map.clear()
    }
}
