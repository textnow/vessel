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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import com.textnow.android.vessel.model.SimpleData
import com.textnow.android.vessel.model.firstSimple
import com.textnow.android.vessel.model.mapped
import com.textnow.android.vessel.model.secondSimple
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Validate functionality of the Vessel implementation.
 * These tests will rely on the in-memory room database so we can verify proper serialization.
 *
 * This test relies on InstantTaskExecutorRule, which breaks Room @Transactions; so it is being
 * tested separately.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LiveDataTest : BaseVesselTest() {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `livedata receives all updates for the correct type`() = runBlocking {
        val events = mutableListOf<SimpleData>()
        val livedata = vessel.livedata(SimpleData::class)
        val observer = Observer<SimpleData?> {
            it?.let { events.add(it) }
        }

        try {
            livedata.observeForever(observer)
            vessel.set(firstSimple)
            vessel.set(mapped)
            vessel.set(secondSimple)
        } finally {
            livedata.removeObserver(observer)
        }

        assertThat(events).all {
            hasSize(2)
            containsExactly(firstSimple, secondSimple)
        }
    }
}
