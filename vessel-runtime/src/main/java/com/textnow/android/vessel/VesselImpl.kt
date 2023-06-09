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
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

/**
 * Vessel provides a container for your data.
 * It is recommended to use one instance per `name`. Consider using DI.
 *
 * @param appContext application context
 * @param name unique name of your vessel
 * @param inMemory true if the database should be in-memory only [Default: false]
 * @param allowMainThread to allow calls to be made on the main thread. [Default: false]
 * @param callback for notifications of database state changes
 * @param cache Optional [VesselCache]. Built-ins: [DefaultCache] and [LruCache] [Default: null]
 */
class VesselImpl(
    private val appContext: Context,
    private val name: String = "vessel-db",
    private val inMemory: Boolean = false,
    private val allowMainThread: Boolean = false,
    private val callback: VesselCallback? = null,
    private val cache: VesselCache? = null,
    private val profile: Boolean = false,
) : Vessel {
    private val TAG = "Vessel"

    /**
     * Indicates a null value - use in place of actual null when caching a null value in [VesselCache]
     * Some cache implementations may not allow storing null directly (ex, a [VesselCache] built using ConcurrentHashMap)
     */
    companion object {
        internal val nullValue = object {}
    }


    // region initialization

    constructor(
        appContext: Context,
        name: String = "vessel-db",
        inMemory: Boolean = false,
        allowMainThread: Boolean = false,
        callback: VesselCallback? = null,
        cache: VesselCache? = null,
    ) : this(appContext, name, inMemory, allowMainThread, callback, cache, false)

    /**
     * Gson instance used for serializing data objects into the database
     */
    private val gson: Gson = GsonBuilder()
        .enableComplexMapKeySerialization()
        // other configuration
        .create()

    /**
     * Underlying Room instance.
     */
    private val db: VesselDb = when (inMemory) {
        true -> Room.inMemoryDatabaseBuilder(appContext, VesselDb::class.java)
        false -> Room.databaseBuilder(appContext, VesselDb::class.java, name)
    }
        .apply {
            enableMultiInstanceInvalidation()
            if (allowMainThread) {
                allowMainThreadQueries()
            }
            callback?.let {
                addCallback(it)
            }
            // Example:
//                addMigrations(VesselMigration(1,2){ migration, db ->
//                    logd("migrating from ${migration.startVersion} -> ${migration.endVersion}")
//                })
        }
        .build()

    /**
     * Room DAO
     */
    private val dao: VesselDao = db.vesselDao()

    /**
     * As opposed to db.isOpen, this keeps track of whether you called a method after calling close().
     * Useful for debugging unit tests.
     */
    private var closeWasCalled: Boolean = false

    private val profiler: Profiler = when {
        profile -> ProfilerImpl()
        else -> DummyProfiler()
    }

    private fun preloadImpl(entities: List<VesselEntity>) {
        for (entity in entities) {
            val kclass = try {
                Class.forName(entity.type).kotlin
            } catch (error: Exception) {
                continue
            }

            val data = if (entity.data != null) fromJson(entity.data, kclass) else nullValue
            cache?.set(entity.type, data as Any)
        }
    }

    override suspend fun preload() {
        if (cache == null) {
            return
        }

        profiler.time(Span.PRELOAD_FROM_DB) {
            val entities = dao.getAll()
            preloadImpl(entities)
        }
    }

    override fun preloadBlocking() {
        if (cache == null) {
            return
        }

        profiler.timeBlocking(Span.PRELOAD_FROM_DB) {
            val entities = dao.getAllBlocking()
            preloadImpl(entities)
        }
    }

    // endregion

    // region profiling

    override val profileData
        get() = profiler.snapshot

    // endregion

    // region helper functions

    /**
     * Convert a stored json string into the specified data type.
     */
    private fun <T : Any> fromJson(value: String, type: KClass<T>): T? = gson.fromJson(value, type.java)

    /**
     * Convert a specified data type into a json string for storage.
     */
    private fun <T : Any> toJson(value: T) = gson.toJson(value)

    /**
     * Get the type of the specified data.
     */
    @VisibleForTesting
    override fun <T : Any> typeNameOf(value: T) = value.javaClass.kotlin.qualifiedName
        ?: throw AssertionError("anonymous classes not allowed. their names will change if the parent code is changed.")

    /**
     * Close the database instance.
     */
    @VisibleForTesting
    override fun close() {
        db.close()
        callback?.onClosed?.invoke()
        closeWasCalled = true
    }

    // endregion

    // region cache helpers

    /**
     * Looks up a type in the cache
     *
     * Returns a [Pair], where [Pair.first] indicates if the type was found in the cache and
     * [Pair.second] contains its value.
     *
     * null is a valid value for [Pair.second] and indicates that the type is known to not exist
     * in the database.  This can be used to avoid going to the database to determine if the value exists
     */
    private fun <T : Any> findCached(type: KClass<T>): Pair<Boolean, T?> {
        val typeName = type.qualifiedName ?: return Pair(false, null)

        return when (val lookup = cache?.get<T>(typeName)) {
            nullValue -> Pair(true, null)
            null -> Pair(false, null)
            else -> Pair(true, lookup)
        }
    }

    /**
     * Returns true if passed data already exists in the cache, which must mean the same
     * value already exists in the database
     *
     * This can be used to avoid rewriting the same data back to the database.
     *
     * For optimal performance [T] should implement [Object.equals]. Types not implementing equals
     * cannot be compared for equality, meaning all writes for that type will always go to the database
     */
    private fun <T : Any> inCache(data: T): Boolean {
        val (exists, value) = findCached(data.javaClass.kotlin)

        if (exists && value == data) {
            return true
        }

        return false
    }
    // endregion

    // region blocking accessors

    /**
     * Get the data of a given type.
     *
     * @param type of data class to lookup
     * @return the data, or null if it does not exist
     */
    override fun <T : Any> getBlocking(type: KClass<T>): T? {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }

        val (exists, value) = findCached(type)
        if (exists) {
            profiler.countBlocking(Event.CACHE_HIT_READ)
            return value
        }

        val typeName = type.qualifiedName ?: return null

        return profiler.timeBlocking(Span.READ_FROM_DB) {
            dao.getBlocking(typeName)
        }?.data.let {
            val data = if (it != null) fromJson(it, type) else null
            cache?.set(typeName, data ?: nullValue)
            data
        } ?: run {
            cache?.set(typeName, nullValue)
            null
        }
    }

    /**
     * Get the data of a given type.
     *
     * @param type of data class to lookup
     * @return the data, or null if it does not exist
     */
    override fun <T : Any> getBlocking(type: Class<T>): T? {
        return getBlocking(type.kotlin)
    }

    /**
     * Set the specified data.
     *
     * @param value of the data class to set/replace.
     */
    override fun <T : Any> setBlocking(value: T) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }

        if (inCache(value)) {
            profiler.countBlocking(Event.CACHE_HIT_WRITE)
            return
        }

        typeNameOf(value).let {
            profiler.timeBlocking(Span.WRITE_TO_DB) {
                dao.setBlocking(
                    entity = VesselEntity(
                        type = it,
                        data = toJson(value)
                    )
                )
            }
            cache?.set(it, value)
        }
    }

    /**
     * Delete the specified data.
     *
     * @param type of the data class to remove.
     */
    override fun <T : Any> deleteBlocking(type: KClass<T>) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }

        val (exists, value) = findCached(type)

        if (exists && value == null) {
            profiler.countBlocking(Event.CACHE_HIT_DELETE)
            return
        }

        type.qualifiedName?.let { typeName ->
            profiler.timeBlocking(Span.DELETE_FROM_DB) {
                dao.deleteBlocking(typeName)
            }
            cache?.set(typeName, nullValue)
        }
    }

    /**
     * Delete the specified data.
     *
     * @param type of the data class to remove.
     */
    override fun <T : Any> deleteBlocking(type: Class<T>) {
        deleteBlocking(type.kotlin)
    }

    // endregion

    // region suspend accessors

    /**
     * Get the data of a given type, in a suspend function.
     *
     * @param type of data class to lookup
     * @return the data, or null if it does not exist
     */
    override suspend fun <T : Any> get(type: KClass<T>): T? {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }

        val (exists, value) = findCached(type)

        if (exists) {
            profiler.count(Event.CACHE_HIT_READ)
            return value
        }

        val typeName = type.qualifiedName ?: return null

        return profiler.time(Span.READ_FROM_DB) {
            dao.get(typeName)
        }?.data.let {
            val data = if (it != null) fromJson(it, type) else null
            cache?.set(typeName, data ?: nullValue)
            data
        } ?: run {
            cache?.set(typeName, nullValue)
            null
        }
    }


    /**
     * Set the specified data, in a suspend function.
     *
     * @param value of the data class to set/replace.
     */
    override suspend fun <T : Any> set(value: T) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }

        if (inCache(value)) {
            profiler.count(Event.CACHE_HIT_WRITE)
            return
        }

        typeNameOf(value).let { typeName ->
            profiler.time(Span.WRITE_TO_DB) {
                dao.set(
                    entity = VesselEntity(
                        type = typeName,
                        data = toJson(value)
                    )
                )
            }
            cache?.set(typeName, value)
        }
    }

    /**
     * Delete the specified data, in a suspend function.
     *
     * @param type of the data class to remove.
     */
    override suspend fun <T : Any> delete(type: KClass<T>) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }

        val (exists, value) = findCached(type)

        if (exists && value == null) {
            profiler.count(Event.CACHE_HIT_DELETE)
            return
        }

        type.qualifiedName?.let { typeName ->
            profiler.time(Span.DELETE_FROM_DB) {
                dao.delete(typeName)
            }
            cache?.set(typeName, nullValue)
        }
    }

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
    override suspend fun <OLD : Any, NEW : Any> replace(old: OLD, new: NEW) {
        replace(old::class, new)
    }

    /**
     * Replace one data with another, in a suspending transaction.
     *
     * @param oldType of data model to remove
     * @param new data model to add
     */
    override suspend fun <OLD : Any, NEW : Any> replace(
        oldType: KClass<OLD>,
        new: NEW
    ) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }

        val newName = typeNameOf(new)

        oldType.qualifiedName?.let { oldName ->
            if (oldName == newName) {
                set(new)
            } else {
                val (oldExists, oldValue) = findCached(oldType)
                if (inCache(new) && oldExists && oldValue == null) {
                    profiler.count(Event.CACHE_HIT_REPLACE)
                    return
                }

                profiler.time(Span.REPLACE_IN_DB) {
                    dao.replace(
                        oldType = oldName,
                        new = VesselEntity(
                            type = newName,
                            data = toJson(new)
                        )
                    )
                }

                /** Note - caching the result of the replace is safe, as any transactional Dao calls will throw on failure
                 * This prevents the cache from getting out of sync with the database
                 * This can be seen by decompiling a generated Room Dao, or somewhat by checking the Room source code generator
                 * (https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:room/)
                 */
                cache?.set(oldName, nullValue)
                cache?.set(newName, new)
            }
        }
    }

    /**
     * Clear the database.
     */
    override fun clear() {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }

        cache?.clear()

        profiler.timeBlocking(Span.CLEAR_DB) {
            db.clearAllTables()
        }
    }

    // endregion


    // region observers

    /**
     * Observe the distinct values of a given type, as a flow.
     *
     * @param type of data class to lookup
     * @return flow of the values associated with that type
     */
    override fun <T : Any> flow(type: KClass<T>): Flow<T?> {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        type.qualifiedName?.let { typeName ->
            return dao.getFlow(typeName)
                .distinctUntilChanged()
                .map {
                    it?.data?.let { entity ->
                        val data = fromJson(entity, type)
                        cache?.set(typeName, data as Any)
                        data
                    }
                }
        }
        return emptyFlow()
    }

    /**
     * Observe the distinct values of a given type, as a livedata.
     *
     * @param type of data class to lookup
     * @return livedata of the values associated with that type
     */
    override fun <T : Any> livedata(type: KClass<T>): LiveData<T?> {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        type.qualifiedName?.let { typeName ->
            return dao.getLiveData(typeName)
                .distinctUntilChanged()
                .map {
                    it?.data?.let { entity ->
                        val data = fromJson(entity, type)
                        cache?.set(typeName, data as Any)
                        data
                    }
                }
        }
        return MutableLiveData<T>()
    }

    // endregion
}
