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

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isLessThan
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.textnow.android.vessel.model.*
import kotlinx.coroutines.runBlocking
import org.koin.core.parameter.parametersOf
import org.koin.test.get

/**
 * Cache for testing.
 *
 * Implements slow slow inserts to allow testing preload timeouts
 *
 * Note:  This relies on test execution times being sane with respect to wall clock time.
 *        Anything more precise would require extensive mocking, so this should be OK.
 */
class SlowCache(val insertDelayMS: Long) : DefaultCache() {
    var setCount = 0

    override fun <T : Any> set(key: String, value: T, fromPreload: Boolean) {
        setCount++
        Thread.sleep(insertDelayMS)
        super.set(key, value, fromPreload)
    }
}

/**
 * Ensure preload timeouts work as expected, and that the resulting data is still usable and correct
 */
abstract class BasePreloadTimeoutTest(private val async: Boolean): BaseVesselTest<VesselCache>(DefaultCache(), true, false) {
    private val slowCache = SlowCache(100)
    private val slowVessel by lazy{VesselWrapper(get<Vessel>{parametersOf(slowCache, true, false, null)}, async)}

    @Test
    fun `preload allows null timeout`(): Unit = runBlocking {
        assertThat {
            vessel.preload(null)
        }.isSuccess()
    }

    @Test
    fun `preload aborts when timeout is exceeded`() = runBlocking {
        vessel.set(firstSimple)
        vessel.set(firstSimpleV2)

        val numberOfEntries = 2

        // 2 entries to preload -> 2x cache insert time
        // Set timeout to << insert time
        val preloadReport = slowVessel.preload(slowCache.insertDelayMS.toInt() / numberOfEntries)

        // Only one entry should load into cache before timeout is hit
        assertThat(slowCache.setCount).isEqualTo(1)

        // Will hit cache
        val data1 = slowVessel.get(SimpleData::class)

        // Will go to disk
        val data1v2 = slowVessel.get(SimpleDataV2::class)

        val profileData = slowVessel.profileData!!

        // Timeout should be hit
        assertThat(preloadReport.timedOut).isTrue()
        assertThat(profileData.hitCountOf(Event.PRELOAD_TIMEOUT)).isEqualTo(1)
        // Cache hit
        assertThat(profileData.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(1)
        // Go to disk
        assertThat(profileData.hitCountOf(Span.READ_FROM_DB)).isEqualTo(1)
        // Second disk read cached
        assertThat(slowCache.setCount).isEqualTo(2)
        // Timeout was hit in a sane amount of time
        assertThat(profileData.timeIn(Span.PRELOAD_FROM_DB)).isLessThan(slowCache.insertDelayMS*numberOfEntries)

        // Sanity
        assertThat(data1).isEqualTo(firstSimple)
        assertThat(data1v2).isEqualTo(firstSimpleV2)
    }

    @Test
    fun `preload does not abort when timeout is not exceeded`() = runBlocking {
        vessel.set(firstSimple)
        vessel.set(firstSimpleV2)

        val numberOfEntries = 2

        // 2 entries to prelaod -> 2x cache insertion time
        // Set timeout to >> insertion time
        val preloadReport = slowVessel.preload(slowCache.insertDelayMS.toInt()*numberOfEntries*2)

        // Both entries should be cached
        assertThat(slowCache.setCount).isEqualTo(2)

        // cache hit for both
        val data1 = slowVessel.get(SimpleData::class)
        val data1v2 = slowVessel.get(SimpleDataV2::class)

        val profileData = slowVessel.profileData!!

        // Timeout should not be hit
        assertThat(preloadReport.timedOut).isFalse()
        assertThat(profileData.hitCountOf(Event.PRELOAD_TIMEOUT)).isEqualTo(0)
        // Cache hit both reads
        assertThat(profileData.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(2)
        // Does not go to disk
        assertThat(profileData.hitCountOf(Span.READ_FROM_DB)).isEqualTo(0)

        // Sanity
        assertThat(data1).isEqualTo(firstSimple)
        assertThat(data1v2).isEqualTo(firstSimpleV2)
    }
}


@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class SyncPreloadTimeoutTest: BasePreloadTimeoutTest(false)

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class AsyncPreloadTimeoutTest: BasePreloadTimeoutTest(true)
