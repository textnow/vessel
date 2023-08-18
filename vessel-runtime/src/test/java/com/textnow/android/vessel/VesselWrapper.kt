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

import kotlin.reflect.KClass

/**
 * Wraps the basic Vessel get/set/delete operations, so this test can reuse the same
 * code to test the blocking and suspend functions
 */
class VesselWrapper(private val vessel: Vessel, private val async: Boolean = false) {
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

    suspend fun preload(timeoutMS: Int?): PreloadReport {
        if (async) {
            return vessel.preload(timeoutMS)
        } else {
            return vessel.preloadBlocking(timeoutMS)
        }
    }

    val profileData get() = vessel.profileData
}