package com.textnow.android.vessel

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

open class DefaultVesselCache : VesselCache {
    private val hashMap = ConcurrentHashMap<String, Any>()

    override val size: Int
        get() = hashMap.size

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String): T? {
        return hashMap[key] as? T?
    }

    override fun <T : Any> set(key: String, value: T) {
        hashMap[key] = value
    }

    override fun remove(key: String) {
        hashMap.remove(key)
    }

    override fun clear() {
        hashMap.clear()
    }
}

open class LRUVesselCache(
    private val capacity: Int
) : VesselCache {
    private val backingCache: VesselCache = DefaultVesselCache()

    // front is the LRU key; end is the most recent used key
    private val evictionOrder = ConcurrentLinkedDeque<String>()

    override val size: Int
        get() = backingCache.size

    override fun <T : Any> get(key: String): T? {
        return backingCache.get<T>(key)?.also {
            moveToEnd(key)
        }
    }

    override fun <T : Any> set(key: String, value: T) {
        moveToEnd(key)
        backingCache.set(key, value)

        if (capacity > 0 && backingCache.size > capacity) {
            val toRemove = evictionOrder.removeFirst()
            backingCache.remove(toRemove)
        }
    }

    override fun remove(key: String) {
        evictionOrder.remove(key)
        backingCache.remove(key)
    }

    override fun clear() {
        backingCache.clear()
        evictionOrder.clear()
    }

    private fun moveToEnd(key: String) {
        evictionOrder.remove(key)
        evictionOrder.addLast(key)
    }

}