package com.textnow.android.vessel

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

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take

import com.textnow.android.vessel.model.*
import org.junit.Rule

class TestCache : DefaultCache() {
    val numberOfSetCalls = HashMap<String, Int>()

    override fun <T : Any> set(key: String, value: T) {
        numberOfSetCalls[key] = numberOfSetCalls.getOrDefault(key, 0) + 1
        super.set(key, value)
    }
}

/**
 * Ensure data integrity when a [VesselCache] is used, and that the cache is being used to optimize
 * get/set/delete/replace calls
 */
abstract class BaseVesselCacheUsageTest(async: Boolean) : BaseVesselTest<TestCache>(TestCache(), true) {



    // vessel is injected in base class, defer init
    private val vesselWrapper by lazy { VesselWrapper(vessel, async) }

    @Test
    fun `already cached data is not read from database again`() = runBlocking {
        cache!!.set(SimpleData::class.qualifiedName!!, firstSimple)

        vesselWrapper.get(SimpleData::class)

        val profData = vessel.profileData


        assertThat(profData?.hitCountOf(Span.READ_FROM_DB)).isEqualTo(0)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(1)
    }

    @Test
    fun `already cached data is not written to database again`() = runBlocking {
        cache!!.set(SimpleData::class.qualifiedName!!, firstSimple)
        
        vesselWrapper.set(firstSimple)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(0)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_WRITE)).isEqualTo(1)
    }

    @Test
    fun `already cached data is not deleted in database again`() = runBlocking {
        vesselWrapper.delete(SimpleData::class)
        vesselWrapper.delete(SimpleData::class)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.DELETE_FROM_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_DELETE)).isEqualTo(1)
    }

    @Test
    fun `database reads are cached`() = runBlocking {
        vesselWrapper.get(SimpleData::class)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.READ_FROM_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(0)
        assertThat(cache!!.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
    }

    @Test
    fun `database writes are cached`() = runBlocking {
        vesselWrapper.set(firstSimple)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_WRITE)).isEqualTo(0)
        assertThat(cache!!.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(firstSimple)
    }

    @Test
    fun `database deletes are cached (as null)`() = runBlocking {
        vesselWrapper.delete(SimpleData::class)
        val read = vesselWrapper.get(SimpleData::class)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.DELETE_FROM_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_DELETE)).isEqualTo(0)
        assertThat(cache!!.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
        assertThat(read).isNull()
    }

    @Test
    fun `changing values are propagated to cache and db`() = runBlocking{
        vesselWrapper.set(firstSimple)
        val read1 = vesselWrapper.get(SimpleData::class)
        vesselWrapper.set(secondSimple)
        val read2 = vesselWrapper.get(SimpleData::class)

        val profData = vessel.profileData

        assertThat(firstSimple).isEqualTo(read1)
        assertThat(secondSimple).isEqualTo(read2)
        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(2)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(2)
    }

    @Test
    fun `new deletes are propagated to cache and db`() = runBlocking{
        vesselWrapper.set(firstSimple)
        val read1 = vesselWrapper.get(SimpleData::class)
        vesselWrapper.delete(SimpleData::class)
        val read2 = vesselWrapper.get(SimpleData::class)

        val profData = vessel.profileData

        assertThat(cache!!.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
        assertThat(firstSimple).isEqualTo(read1)
        assertThat(null).isEqualTo(read2)
        assertThat(profData?.hitCountOf(Span.DELETE_FROM_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(2)
    }
}

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class AsyncVesselCacheUsageTest: BaseVesselCacheUsageTest(true)

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class SyncVesselCacheUsageTest: BaseVesselCacheUsageTest(false)

/**
 * Ensure data integrity when a [VesselCache] is used, and that the cache is being used to optimize
 * get/set/delete/replace calls
 */
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class AsyncOnlyVesselCacheUsageTest: BaseVesselTest<TestCache>(TestCache(), true) {
    @Test
    fun `replace with new type is cached`() = runBlocking {
        vessel.set(firstSimple)
        // Note - this will deadlock if using @Rule with InstantTaskExecutorRule.  All other
        // suspend functions still work fine
        // Perhaps some variation on https://issuetracker.google.com/issues/120854786
        vessel.replace(SimpleData::class, firstSimpleV2)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.REPLACE_IN_DB)).isEqualTo(1)
        assertThat(cache!!.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
        assertThat(cache.get<SimpleDataV2>(SimpleDataV2::class.qualifiedName!!)).isEqualTo(firstSimpleV2)
    }

    @Test
    fun `replace with same type is equivalent to set`() = runBlocking {
        vessel.set(firstSimple)
        // Note:  This will deadlock if using InstantTaskExecutorRule
        vessel.replace(SimpleData::class, secondSimple)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(2)
        assertThat(cache!!.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(secondSimple)
    }

    @Test
    fun `already cached data is not replaced in database`() = runBlocking {
        vessel.set(firstSimple)
        // Note:  This will deadlock if using InstantTaskExecutorRule
        vessel.replace(SimpleData::class, firstSimpleV2)
        vessel.replace(SimpleData::class, firstSimpleV2)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_REPLACE)).isEqualTo(1)
        assertThat(cache!!.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
    }

}


/**
 * Ensure data integrity when a [VesselCache] is used, and that the cache is being used to optimize
 * get/set/delete/replace calls
 */
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class ObserversVesselCacheUsageTest: BaseVesselTest<TestCache>(TestCache()) {
    // Note:  This will deadlock certain transactional DAO methods, such as replace (but not set/delete)
    @get:Rule
    val executor = InstantTaskExecutorRule()

    @Test
    fun `flow reads are cached`() = runBlocking {
        vessel.set(firstSimple)

        val ld = vessel.flow(SimpleData::class)

        var read: SimpleData? = null
        ld.take(1).collect {
            read = it
        }

        assertThat(read).isEqualTo(firstSimple)
        assertThat(cache!!.numberOfSetCalls.getOrDefault(SimpleData::class.qualifiedName, 0)).isEqualTo(2)
        assertThat(cache.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(firstSimple)
    }

    @Test
    fun `livedata reads are cached`() = runBlocking {
        val ld = vessel.livedata(SimpleData::class)

        var read: SimpleData? = null
        val observer = Observer<SimpleData?> {
            read = it
        }

        ld.observeForever(observer)
        vessel.set(firstSimple)

        ld.removeObserver(observer)

        assertThat(cache!!.numberOfSetCalls.getOrDefault(SimpleData::class.qualifiedName, 0)).isEqualTo(2)
        assertThat(read).isEqualTo(firstSimple)
        assertThat(cache.get<SimpleData>(SimpleData::class.qualifiedName!!)).isEqualTo(firstSimple)
    }
}