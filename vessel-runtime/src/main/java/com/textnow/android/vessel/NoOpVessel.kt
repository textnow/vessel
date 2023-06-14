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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.reflect.KClass

/**
 * No-Op implementation of Vessel.
 */
@VisibleForTesting
class NoOpVessel : Vessel {
    override fun <T : Any> typeNameOf(value: T): String = "no-op"
    override fun close() { /* no-op */ }
    override suspend fun preload(timeoutMS: Int?) { /* no-op */ }
    override fun preloadBlocking(timeoutMS: Int?) { /* no-op */ }

    override val profileData: ProfileData? = null
    override fun <T : Any> getBlocking(type: KClass<T>): T? = null
    override fun <T : Any> getBlocking(type: Class<T>): T? = null
    override fun <T : Any> setBlocking(value: T) { /* no-op */ }
    override fun <T : Any> deleteBlocking(type: Class<T>) { /* no-op */ }
    override fun <T : Any> deleteBlocking(type: KClass<T>) { /* no-op */ }
    override suspend fun <T : Any> get(type: KClass<T>): T? = null
    override suspend fun <T : Any> set(value: T) { /* no-op */ }
    override suspend fun <T : Any> delete(type: KClass<T>) { /* no-op */ }
    override suspend fun <OLD : Any, NEW : Any> replace(old: OLD, new: NEW) { /* no-op */ }
    override suspend fun <OLD : Any, NEW : Any> replace(oldType: KClass<OLD>, new: NEW) { /* no-op */ }
    override fun clear() { /* no-op */ }
    override fun <T : Any> flow(type: KClass<T>): Flow<T?> = emptyFlow()
    override fun <T : Any> livedata(type: KClass<T>): LiveData<T?> = liveData {  }
}
