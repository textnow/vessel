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
package com.textnow.android.vessel.di

import com.textnow.android.vessel.DefaultVesselCache
import com.textnow.android.vessel.Vessel
import com.textnow.android.vessel.VesselCallback
import com.textnow.android.vessel.VesselImpl
import org.koin.dsl.module

/**
 * Koin module to be reset during each @Test.
 */
val testModule = module {
    single<Vessel> {
        VesselImpl(
            appContext = get(),
            inMemory = true,
            allowMainThread = true,
            callback = VesselCallback(
                onCreate = { println("Database created") },
                onOpen = { println("Database opened") },
                onClosed = { println("Database closed") },
                onDestructiveMigration = { println("Destructive migration") }
            ),
            cache = DefaultVesselCache()
        )
    }
}
