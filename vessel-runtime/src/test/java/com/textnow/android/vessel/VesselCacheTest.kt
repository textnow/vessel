package com.textnow.android.vessel

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isZero
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

abstract class BaseVesselCacheTest<T : VesselCache> {
    private lateinit var cache : T

    abstract fun getInstance() : T

    @Before
    fun setUp() {
        cache = getInstance()
    }

    @After
    fun teardown() {
        cache.clear()
    }

    @Test
    fun `set the correct data for cache key`() {
        cache.set("key", 3)
        assertThat(cache.get<Int>("key")).isEqualTo(3)
    }

    @Test
    fun `set changes value for same cache key`() {
        cache.set("key", 5)
        cache.set("key", 7)
        assertThat (cache.get<Int>("key")).isEqualTo(7)
        assertThat(cache.size).isEqualTo(1)
    }

    @Test
    fun `remove removes the data from the cache`() {
        cache.set("key", 3)
        cache.set("key2", 5)

        cache.remove("key2")
        assertThat (cache.get<Int>("key2")).isNull()
        assertThat(cache.size).isEqualTo(1)

        cache.remove("key")
        assertThat(cache.get<Int>("key")).isNull()
        assertThat(cache.size).isZero()
    }

    @Test
    fun `remove nonexistent produces no error`() {
        // should not error
        cache.remove("test")
    }

    @Test
    fun `get produces correct values`() {
        cache.set("key", 3)
        cache.set("key", 4)
        cache.set("other", 5)
        assertThat(cache.get<Int>("key")).isEqualTo(4)
        assertThat(cache.get<Int>("other")).isEqualTo(5)
        assertThat(cache.size).isEqualTo(2)
    }

    @Test
    fun `get non-existent key produces null`() {
        assertThat(cache.get<Int>("key")).isNull()
    }

    @Test
    fun `clear removes all data`() {
        assertThat(cache.size).isZero()
        cache.set("key", 5)
        cache.set("key2", 6)
        cache.set("key", 7)
        assertThat(cache.size).isEqualTo(2)
        cache.clear()
        assertThat(cache.size).isEqualTo(0)
        assertThat(cache.get<Int>("key")).isNull()
    }

}

@RunWith(JUnit4::class)
class DefaultVesselCacheTest : BaseVesselCacheTest<DefaultVesselCache>() {
    override fun getInstance(): DefaultVesselCache {
        return DefaultVesselCache()
    }
}

@RunWith(JUnit4::class)
class LRUVesselCacheTest : BaseVesselCacheTest<LRUVesselCache>() {
    override fun getInstance(): LRUVesselCache {
        return LRUVesselCache(100)
    }

    @Test
    fun `evicts LRU key when cache reaches capacity`() {
        val lruCache = LRUVesselCache(3)
        lruCache.set("key", 1)
        lruCache.set("key2", 2)
        lruCache.set("key3", 3)
        assertThat(lruCache.size).isEqualTo(3)
        lruCache.set("key4", 4)
        assertThat(lruCache.size).isEqualTo(3)
        assertThat(lruCache.get<Int>("key")).isNull()

        // move key3 as recently used key
        lruCache.get<Int>("key3")
        lruCache.set("key5", 5)
        assertThat(lruCache.size).isEqualTo(3)
        assertThat(lruCache.get<Int>("key2")).isNull()
        assertThat(lruCache.get<Int>("key5")).isEqualTo(5)
    }
}