/**
 * MIT License
 *
 * Copyright (c) 2020 TextNow, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
class DefaultCacheTest : BaseVesselCacheTest<DefaultCache>() {
    override fun getInstance(): DefaultCache {
        return DefaultCache()
    }
}

@RunWith(JUnit4::class)
class LruCacheTest : BaseVesselCacheTest<LruCache>() {
    override fun getInstance(): LruCache {
        return LruCache(100)
    }

    @Test
    fun `evicts LRU key when cache reaches capacity`() {
        val lruCache = LruCache(3)
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