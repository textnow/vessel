package com.textnow.android.vessel

/**
 * MIT License
 *
 * Copyright (c) 2020 TextNow, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including witho`Fƒut limitation the rights
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
import kotlin.reflect.KClass

import com.textnow.android.vessel.model.*
import org.junit.Rule

class TestCache : DefaultCache() {
    val numberOfSetCalls = HashMap<String, Int>()

    override fun <T : Any> set(key: String, value: T) {
        numberOfSetCalls[key] = numberOfSetCalls.getOrDefault(key, 0) + 1
        super.set(key, value)
    }
}

data class Data1(val field: Int = 1)
val data1 = Data1()

data class Data2(val field: Int = 2)
val data2 = Data2()

abstract class BaseVesselCacheUsageTest(async: Boolean) : BaseVesselTest<TestCache>(TestCache(), true) {

    /**
     * Wraps the basic Vessel get/set/delete operations, so this test can reuse the same
     * code to test the blocking and suspend functions
     */
    class BasicVessel(private val vessel: Vessel, private val async: Boolean = false) {
        suspend fun <T: Any> get(type: KClass<T>): T? {
            return if (async) {
                vessel.get(type)
            } else {
                vessel.getBlocking(type)
            }
        }

        suspend fun set(value: Any) {
            if (async) {
                vessel.set(value)
            } else {
                vessel.setBlocking(value)
            }
        }

        suspend fun <T: Any> delete(type: KClass<T>) {
            if (async) {
                vessel.delete(type)
            } else {
                vessel.deleteBlocking(type)
            }
        }
    }

    // vessel is injected in base class, defer init
    private val basicVessel by lazy { BasicVessel(vessel, async) }

    @Test
    fun `already cached data is not read from database again`() = runBlocking {
        cache!!.set(Data1::class.qualifiedName!!, data1)

        basicVessel.get(Data1::class)

        val profData = vessel.profileData


        assertThat(profData?.hitCountOf(Span.READ_FROM_DB)).isEqualTo(0)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(1)
    }

    @Test
    fun `already cached data is not written to database again`() = runBlocking {
        cache!!.set(Data1::class.qualifiedName!!, data1)
        
        basicVessel.set(data1)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(0)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_WRITE)).isEqualTo(1)
    }

    @Test
    fun `already cached data is not deleted in database again`() = runBlocking {
        basicVessel.delete(Data1::class)
        basicVessel.delete(Data1::class)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.DELETE_FROM_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_DELETE)).isEqualTo(1)
    }

    @Test
    fun `database reads are cached`() = runBlocking {
        basicVessel.get(Data1::class)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.READ_FROM_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(0)
        assertThat(cache!!.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
    }

    @Test
    fun `databasde writes are cached`() = runBlocking {
        basicVessel.set(data1)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_WRITE)).isEqualTo(0)
        assertThat(cache!!.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(data1)
    }

    @Test
    fun `database deletes are cached (as null)`() = runBlocking {
        basicVessel.delete(Data1::class)
        val read = basicVessel.get(Data1::class)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.DELETE_FROM_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_DELETE)).isEqualTo(0)
        assertThat(cache!!.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
        assertThat(read).isNull()
    }

    @Test
    fun `changing values are propagated to cache and db`() = runBlocking{
        basicVessel.set(Data1(1))
        val read1 = basicVessel.get(Data1::class)
        basicVessel.set(Data1(2))
        val read2 = basicVessel.get(Data1::class)

        val profData = vessel.profileData

        assertThat(Data1(1)).isEqualTo(read1)
        assertThat(Data1(2)).isEqualTo(read2)
        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(2)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(2)
    }

    @Test
    fun `new deletes are propagated to cache and db`() = runBlocking{
        basicVessel.set(data1)
        val read1 = basicVessel.get(Data1::class)
        basicVessel.delete(Data1::class)
        val read2 = basicVessel.get(Data1::class)

        val profData = vessel.profileData

        assertThat(cache!!.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
        assertThat(data1).isEqualTo(read1)
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

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class AsyncOnlyVesselCacheUsageTest: BaseVesselTest<TestCache>(TestCache(), true) {
    @Test
    fun `replace with new type is cached`() = runBlocking {
        vessel.set(data1)
        // Note - this will deadlock if using @Rule with InstantTaskExecutorRule.  All other
        // suspend functions still work fine
        // Perhaps some variation on https://issuetracker.google.com/issues/120854786
        vessel.replace(Data1::class, data2)

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.REPLACE_IN_DB)).isEqualTo(1)
        assertThat(cache!!.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
        assertThat(cache.get<Data2>(Data2::class.qualifiedName!!)).isEqualTo(data2)
    }

    @Test
    fun `replace with same type is equivalent to set`() = runBlocking {
        vessel.set(data1)
        // Note:  This will deadlock if using InstantTaskExecutorRule
        vessel.replace(Data1::class, Data1(2))

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(2)
        assertThat(cache!!.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(Data1(2))
    }

    @Test
    fun `already cached data is not replaced in database`() = runBlocking {
        vessel.set(data1)
        // Note:  This will deadlock if using InstantTaskExecutorRule
        vessel.replace(Data1::class, Data2(2))
        vessel.replace(Data1::class, Data2(2))

        val profData = vessel.profileData

        assertThat(profData?.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(1)
        assertThat(profData?.hitCountOf(Event.CACHE_HIT_REPLACE)).isEqualTo(1)
        assertThat(cache!!.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(VesselImpl.nullValue)
    }

}


@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class ObserversVesselCacheUsageTest: BaseVesselTest<TestCache>(TestCache()) {
    // Note:  This will deadlock certain transactional DAO methods, such as replace (but not set/delete)
    @get:Rule()
    val executor = InstantTaskExecutorRule()

    @Test
    fun `flow reads are cached`() = runBlocking {
        vessel.set(data1)

        val ld = vessel.flow(Data1::class)

        var read: Data1? = null
        ld.take(1).collect {
            read = it
        }

        assertThat(read).isEqualTo(data1)
        assertThat(cache!!.numberOfSetCalls.getOrDefault(Data1::class.qualifiedName, 0)).isEqualTo(2)
        assertThat(cache.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(data1)
    }

    @Test
    fun `livedata reads are cached`() = runBlocking {
        val ld = vessel.livedata(Data1::class)

        var read: Data1? = null
        val observer = Observer<Data1?> {
            read = it
        }

        ld.observeForever(observer)
        vessel.set(data1)

        ld.removeObserver(observer)

        assertThat(cache!!.numberOfSetCalls.getOrDefault(Data1::class.qualifiedName, 0)).isEqualTo(2)
        assertThat(read).isEqualTo(data1)
        assertThat(cache.get<Data1>(Data1::class.qualifiedName!!)).isEqualTo(data1)
    }
}