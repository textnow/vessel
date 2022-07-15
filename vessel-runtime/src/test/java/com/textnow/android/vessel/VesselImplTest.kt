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
import assertk.assertions.isFailure
import assertk.assertions.isNull
import com.textnow.android.vessel.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Validate functionality of the Vessel implementation.
 * These tests will rely on the in-memory room database so we can verify proper serialization.
 * Tests Vessel with caching and non-caching functionality
 */
abstract class VesselImplTest(cache: VesselCache?): BaseVesselTest(cache) {
    @Test
    fun `reasonable names are chosen for data`() {
        assertThat(vessel.typeNameOf(firstSimple)).isEqualTo("com.textnow.android.vessel.model.SimpleData")
        assertThat(vessel.typeNameOf("")).isEqualTo("kotlin.String")
    }

    @Test
    fun `anonymous classes are not allowed for data`() {
        val lambda: ()->Unit = {}
        assertThat {
            vessel.typeNameOf(lambda)
        }.isFailure()
    }

    @Test
    fun `close prevents further calls`() {
        vessel.setBlocking(firstSimple)
        vessel.close()
        assertThat {
            vessel.getBlocking(SimpleData::class)
        }.isFailure()
    }

    @Test
    fun `round trip serialization works for basic data`() {
        vessel.setBlocking(firstSimple)
        val result = vessel.getBlocking(SimpleData::class)
        assertThat(result).isEqualTo(firstSimple)
        assertThat(result?.name).isEqualTo(firstSimple.name)
    }

    @Test
    fun `round trip serialization works for mapped data`() {
        vessel.setBlocking(mapped)
        val result = vessel.getBlocking(MappedData::class)
        assertThat(result).isEqualTo(mapped)
        assertThat(result?.first()?.name).isEqualTo(mapped.first()?.name)
    }

    @Test
    fun `round trip serialization works for nested data`() {
        vessel.setBlocking(nested)
        val result = vessel.getBlocking(NestedData::class)
        assertThat(result).isEqualTo(nested)
        assertThat(result?.first()?.name).isEqualTo(nested.first()?.name)
    }

    @Test
    fun `delete removes data`() {
        vessel.setBlocking(firstSimple)
        vessel.deleteBlocking(SimpleData::class)
        val result = vessel.getBlocking(SimpleData::class)
        assertThat(result).isNull()
    }

    @Test
    fun `suspending round trip serialization works for basic data`() = runBlocking {
        vessel.set(firstSimple)
        val result = vessel.get(SimpleData::class)
        assertThat(result).isEqualTo(firstSimple)
        assertThat(result?.name).isEqualTo(firstSimple.name)
    }

    @Test
    fun `suspending round trip serialization works for mapped data`() = runBlocking {
        vessel.set(mapped)
        val result = vessel.get(MappedData::class)
        assertThat(result).isEqualTo(mapped)
        assertThat(result?.first()?.name).isEqualTo(mapped.first()?.name)
    }

    @Test
    fun `suspending round trip serialization works for nested data`() = runBlocking {
        vessel.set(nested)
        val result = vessel.get(NestedData::class)
        assertThat(result).isEqualTo(nested)
        assertThat(result?.first()?.name).isEqualTo(nested.first()?.name)
    }

    @Test
    fun `suspending delete removes data`() = runBlocking {
        vessel.set(firstSimple)
        vessel.delete(SimpleData::class)
        val result = vessel.get(SimpleData::class)
        assertThat(result).isNull()
    }

    @Test
    fun `replaced models update correctly`() = runBlocking {
        vessel.set(firstSimple)
        val replacement = SimpleDataV2(
            id = firstSimple.id,
            name = firstSimple.name,
            number = firstSimple.number?.toDouble()
        )
        vessel.replace(old = firstSimple, new = replacement)
        assertThat(vessel.get(SimpleData::class)).isNull()
        assertThat(vessel.get(SimpleDataV2::class)?.number).isEqualTo(replacement.number)

        vessel.replace(oldType = replacement::class, new = firstSimple)
        assertThat(vessel.get(SimpleDataV2::class)).isNull()
        assertThat(vessel.get(SimpleData::class)?.number).isEqualTo(firstSimple.number)
    }

    @Test
    fun `writing the same data type replaces the value`() = runBlocking {
        vessel.set(firstSimple)
        vessel.set(secondSimple)
        assertThat(vessel.get(SimpleData::class)).isEqualTo(secondSimple)
    }

    @Test
    fun `clear removes data`() = runBlocking {
        vessel.set(firstSimple)
        vessel.set(mapped)
        vessel.set(nested)
        vessel.clear()
        assertThat(vessel.get(SimpleData::class)).isNull()
        assertThat(vessel.get(MappedData::class)).isNull()
        assertThat(vessel.get(NestedData::class)).isNull()
    }
}

// Workaround for parameterized tests

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class VesselImplTestNoCache : VesselImplTest(null)

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(AndroidJUnit4::class)
class VesselImplTestCached : VesselImplTest(DefaultCache())
