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
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import com.textnow.android.vessel.model.SimpleData
import com.textnow.android.vessel.model.firstSimple
import com.textnow.android.vessel.model.mapped
import com.textnow.android.vessel.model.secondSimple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Validate functionality of the Vessel implementation.
 * These tests will rely on the in-memory room database so we can verify proper serialization.
 *
 * This test is focused on the flow() callback.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [Build.VERSION_CODES.P],
    manifest = Config.NONE
)
class FlowTest : BaseVesselTest() {
    private val dispatcher = TestCoroutineDispatcher()
    private val scope = TestCoroutineScope(dispatcher)

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher = dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        dispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `flow receives all updates for the correct type`() = scope.runBlockingTest {
        val result = mutableListOf<SimpleData?>()
        val job = launch {
            vessel.flow(SimpleData::class)
                .collect {
                    println("Collected $it")
                    result.add(it)
                }
        }

        assertThat(result).containsExactly(null)

        vessel.set(firstSimple)
        assertThat(result).containsExactly(null, firstSimple)

        vessel.set(mapped)
        assertThat(result).containsExactly(null, firstSimple)

        vessel.set(secondSimple)
        assertThat(result).containsExactly(null, firstSimple, secondSimple)

        job.cancel()
    }
}
