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
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

interface Vessel {

    // region helper functions

    /**
     * Get the type (key) for the specified data.
     */
    @VisibleForTesting
    fun <T : Any> typeNameOf(value: T): String

    /**
     * Close the database instance.
     */
    @VisibleForTesting
    fun close()

    /**
     * Read the entire database into the cache in one operation.
     *
     * This requires a cache.  If no cache was configured, this function does nothing.
     *
     * This can lead to a significant speedup in some scenarios:
     * - When reading many keys
     * - When writing many keys, but the values written match what is already in the database
     *
     * Many random reads can be slower than one larger read.  Similarly one large read is likely
     * faster than re-writing many values.
     *
     * [timeoutMS] should be used when the upper limit on database size is not well understood or is
     * otherwise unknown.  As database size increases, preload time will as well - at some point
     * this may exceed the benefits of preloading.  Use [profileData] to understand these performance
     * tradeoffs against your specific use cases.
     *
     * @param timeoutMS Optional timeout - stop the preload operation once execution time exceeds [timeoutMS]
     */
    suspend fun preload(timeoutMS: Int? = null): PreloadReport

    /**
     * Blocking version of [preload].
     */
    fun preloadBlocking(timeoutMS: Int? = null): PreloadReport

    // endregion

    // region profiling

    /**
     * Profiling data, if profiling was enabled.
     *
     * This can be used to understand the data access patterns and their performance implications.
     *
     * This can be used to inform whether [preload]ing would increase or decrease performance.
     */
    val profileData: ProfileData?

    // endregion

    // region blocking accessors

    /**
     * Get the data of a given type.
     *
     * @param type of data class to lookup
     * @return the data, or null if it does not exist
     */
    fun <T : Any> getBlocking(type: KClass<T>): T?

    /**
     * Get the data of a given type.
     *
     * @param type of data class to lookup
     * @return the data, or null if it does not exist
     */
    fun <T : Any> getBlocking(type: Class<T>): T?

    /**
     * Set the specified data.
     *
     * @param value of the data class to set/replace.
     */
    fun <T : Any> setBlocking(value: T)

    /**
     * Delete the specified data.
     *
     * @param type of data class to lookup
     */
    fun <T : Any> deleteBlocking(type: KClass<T>)

    /**
     * Delete the specified data.
     *
     * @param type of data class to delete
     */
    fun <T : Any> deleteBlocking(type: Class<T>)

    // endregion

    // region suspend accessors

    /**
     * Get the data of a given type, in a suspend function.
     *
     * @param type of data class to lookup
     * @return the data, or null if it does not exist
     */
    suspend fun <T : Any> get(type: KClass<T>): T?

    /**
     * Set the specified data, in a suspend function.
     *
     * @param value of the data class to set/replace.
     */
    suspend fun <T : Any> set(value: T)

    /**
     * Delete the specified data, in a suspend function.
     *
     * @param type of data class to delete
     */
    suspend fun <T : Any> delete(type: KClass<T>)

    // endregion

    // region utilities

    /**
     * Replace one data class with another, in a suspending transaction.
     *
     * @param old data model to remove
     * @param new data model to add
     */
    @Deprecated(
        message = "replacing by passing in an object will be removed in a future version in favour of using class type",
        replaceWith = ReplaceWith("replace(oldType = old::class, new = new)"),
    )
    suspend fun <OLD : Any, NEW : Any> replace(old: OLD, new: NEW)

    /**
     * Replace one data with another, in a suspending transaction.
     *
     * @param oldType of data model to remove
     * @param new data model to add
     */
    suspend fun <OLD : Any, NEW : Any> replace(oldType: KClass<OLD>, new: NEW)

    /**
     * Clear the database.
     */
    fun clear()

    // endregion


    // region observers

    /**
     * Observe the distinct values of a given type, as a flow.
     *
     * @param type of data class to lookup
     * @return flow of the values associated with that type
     */
    fun <T : Any> flow(type: KClass<T>): Flow<T?>

    /**
     * Observe the distinct values of a given type, as a livedata.
     *
     * @param type of data class to lookup
     * @return livedata of the values associated with that type
     */
    fun <T : Any> livedata(type: KClass<T>): LiveData<T?>

    // endregion
}
