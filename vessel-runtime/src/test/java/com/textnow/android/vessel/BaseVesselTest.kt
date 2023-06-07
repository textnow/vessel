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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.textnow.android.vessel.di.testModule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.test.AutoCloseKoinTest
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

/**
 * Base test class to handle starting and stopping Koin,
 * setting up the test module dependency and closing the db.
 */
abstract class  BaseVesselTest<T: VesselCache>(protected val cache: T? = null, protected val profile: Boolean = false) : KoinTest {
    val vessel: Vessel by inject { parametersOf(cache, profile) }

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        androidContext(ApplicationProvider.getApplicationContext<Context>())
        printLogger()

        /**
         * Workaround for https://github.com/InsertKoinIO/koin/issues/871
         * Switch back once [VSL-2] is complete.
         */
        //modules(testModule)
        koin.loadModules(listOf(testModule))
    }
    @After
    fun tearDown() {
        try {
            vessel.clear()
            vessel.close()
        } catch (ise: IllegalStateException) {
            // ignore when the db is already closed
        }
    }

    // Overridden so subclasses don't have to
    override fun getKoin(): Koin {
        return super.getKoin()
    }
}
