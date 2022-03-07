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
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
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
 */
class VesselImpl(
        private val appContext: Context,
        private val name: String = "vessel-db",
        private val inMemory: Boolean = false,
        private val allowMainThread: Boolean = false,
        private val callback: VesselCallback? = null,
        private val cache: VesselCache? = null
): Vessel {
    // region initialization

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
    private val db: VesselDb = when(inMemory) {
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

    // endregion

    // region helper functions

    /**
     * Convert a stored json string into the specified data type.
     */
    private fun <T : Any> fromJson(value: String, type: KClass<T>): T? = gson.fromJson<T>(value, type.java)

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

    // region blocking accessors

    /**
     * Get the data of a given type.
     *
     * @param type of data class to lookup
     * @return the data, or null if it does not exist
     */
    override fun <T : Any> getBlocking(type: KClass<T>): T? {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        type.qualifiedName?.let { typeName ->
            dao.getBlocking(typeName)?.data?.let { entity ->
                return fromJson(entity, type)
            }
        }
        return null
    }

    /**
     * Get the data of a given type.
     *
     * @param type of data class to lookup
     * @return the data, or null if it does not exist
     */
    override fun <T : Any> getBlocking(type: Class<T>): T? {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        type.kotlin.qualifiedName?.let { typeName ->
            dao.getBlocking(typeName)?.data?.let { entity ->
                return fromJson(entity, type.kotlin)
            }
        }
        return null
    }

    /**
     * Set the specified data.
     *
     * @param value of the data class to set/replace.
     */
    override fun <T : Any> setBlocking(value: T) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        typeNameOf(value).let { typeName ->
            dao.setBlocking(entity = VesselEntity(
                    type = typeName,
                    data = toJson(value)
            ))
        }
    }

    /**
     * Delete the specified data.
     *
     * @param type of the data class to remove.
     */
    override fun <T : Any> deleteBlocking(type: KClass<T>) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        type.qualifiedName?.let { typeName ->
            dao.deleteBlocking(typeName)
        }
    }

    /**
     * Delete the specified data.
     *
     * @param type of the data class to remove.
     */
    override fun <T : Any> deleteBlocking(type: Class<T>) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        type.kotlin.qualifiedName?.let { typeName ->
            dao.deleteBlocking(typeName)
        }
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
        type.qualifiedName?.let { typeName ->
            return cache?.get(typeName) ?: dao.get(typeName)?.data?.let { entity ->
                fromJson(entity, type)
            }
        }
        return null
    }

    /**
     * Set the specified data, in a suspend function.
     *
     * @param value of the data class to set/replace.
     */
    override suspend fun <T : Any> set(value: T) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        typeNameOf(value).let { typeName ->
            cache?.set(typeName, value)
            dao.set(entity = VesselEntity(
                type = typeName,
                data = toJson(value)
            ))
        }
    }

    /**
     * Delete the specified data, in a suspend function.
     *
     * @param type of the data class to remove.
     */
    override suspend fun <T : Any> delete(type: KClass<T>) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        type.qualifiedName?.let { typeName ->
            cache?.remove(typeName)
            dao.delete(typeName)
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
    override suspend fun <OLD : Any, NEW : Any> replace(old: OLD, new: NEW) {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        val oldName = typeNameOf(old)
        val newName = typeNameOf(new)
        if (oldName == newName) {
            set(new)
            cache?.set(newName, new)
        } else {
            dao.replace(
                old = VesselEntity(
                    type = oldName,
                    data = toJson(old)),
                new = VesselEntity(
                    type = newName,
                    data = toJson(new))
            )
            cache?.remove(oldName)
            cache?.set(newName, new)
        }
    }

    /**
     * Clear the database.
     */
    override fun clear() {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        db.clearAllTables()
        runBlocking {
            cache?.clear()
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
    @ExperimentalCoroutinesApi
    override fun <T : Any> flow(type: KClass<T>): Flow<T?> {
        check(!closeWasCalled) { "Vessel($name:${hashCode()}) was already closed." }
        type.qualifiedName?.let { typeName ->
            return dao.getFlow(typeName)
                    .distinctUntilChanged()
                    .map {
                        it?.data?.let { entity ->
                            fromJson(entity, type)
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
                            fromJson(entity, type)
                        }
                    }
        }
        return MutableLiveData<T>()
    }

    // endregion
}
