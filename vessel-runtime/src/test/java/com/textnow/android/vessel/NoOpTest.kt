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
import com.textnow.android.vessel.model.SimpleData
import com.textnow.android.vessel.model.firstSimple
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.dsl.module
import org.koin.test.KoinTestRule

/**
 * Validate ability to mock Vessel when an in-memory db is not desired.
 */
@RunWith(JUnit4::class)
class NoOpTest {
    private val vesselMock: Vessel = mockk()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger()

        /**
         * Workaround for https://github.com/InsertKoinIO/koin/issues/871
         * Switch back once [VSL-2] is complete.
         */
        //modules(testModule)
        koin.loadModules(listOf(module {
            single<Vessel> { vesselMock }
        }))
        koin.createRootScope()
    }

    @Test
    fun `mocked data works in junit`() {
        every { vesselMock.getBlocking(SimpleData::class) } returns firstSimple
        assertThat(ClassUnderTest().getName()).isEqualTo(firstSimple.name)
    }
}

private class ClassUnderTest : KoinComponent {
    private val vessel: Vessel by inject()

    fun getName() = vessel.getBlocking(SimpleData::class)?.name
}
