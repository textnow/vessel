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
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.textnow.android.vessel.model.*
import kotlinx.coroutines.runBlocking

/**
 * Ensure preload timeouts work as expected, and that the resulting data is still usable and correct
 */
abstract class ErrorHandlingTest(private val async: Boolean) :
    BaseVesselTest<VesselCache>(
        DefaultCache(),
        true,
        false,
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VesselDb::class.java
        )
            .allowMainThreadQueries()
            .build()
    ) {

    private val vesselWrapper by lazy { VesselWrapper(vessel, async) }
    private val dao = db!!.vesselDao()

    @Test
    fun `preload safely handles, and reports on, unknown types`(): Unit = runBlocking {
        val badType = "com.some.type.DoesNotExist"
        dao.set(VesselEntity(badType, "{}"))

        vesselWrapper.set(firstSimple)

        vesselWrapper.get(SimpleData::class)

        val preloadReport = vesselWrapper.preload(1000)
        val profileData = vesselWrapper.profileData!!

        assertThat(preloadReport.errorsOcurred).isTrue()
        assertThat(preloadReport.missingTypes).isEqualTo(listOf(badType))
        assertThat(profileData.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(1)
        assertThat(profileData.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(1)
        assertThat(profileData.hitCountOf(Event.TYPE_NOT_FOUND)).isEqualTo(1)
    }

    @Test
    fun `preload safely handles, and reports on, invalid data`(): Unit = runBlocking {
        val badType = SimpleDataV2::class.qualifiedName.toString()
        dao.set(VesselEntity(badType, "I'm not JSON"))

        vesselWrapper.set(firstSimple)

        vesselWrapper.get(SimpleData::class)

        val preloadReport = vesselWrapper.preload(1000)
        val profileData = vesselWrapper.profileData!!

        assertThat(preloadReport.errorsOcurred).isTrue()
        assertThat(preloadReport.deserializationErrors).isEqualTo(listOf(badType))
        assertThat(profileData.hitCountOf(Span.WRITE_TO_DB)).isEqualTo(1)
        assertThat(profileData.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(1)
        assertThat(profileData.hitCountOf(Event.DESERIALIZATION_ERROR)).isEqualTo(1)
    }

    @Test
    fun `fetching an unknown type is tracked by the profiler`() = runBlocking {
        val anonymous = object{}

        vesselWrapper.get(anonymous.javaClass.kotlin)

        val profData = vessel.profileData!!
        assertThat(profData.hitCountOf(Event.TYPE_NOT_FOUND)).isEqualTo(1)
    }

    @Test
    fun `fetching invalid data is tracked by the profiler`() = runBlocking {
        val badType = SimpleData::class.qualifiedName.toString()
        dao.set(VesselEntity(badType, "I'm not JSON"))

        try {
            vesselWrapper.get(SimpleData::class)
        } catch (error: Exception) {
            Unit
        }

        val profData = vessel.profileData!!

        assertThat(profData.hitCountOf(Event.DESERIALIZATION_ERROR)).isEqualTo(1)
    }

    }


@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class SyncErrorHandlingTest : ErrorHandlingTest(false)

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class AsncErrorHandling : ErrorHandlingTest(true)
