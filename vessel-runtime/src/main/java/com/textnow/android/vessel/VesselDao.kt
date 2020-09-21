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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Vessel data access.
 * This should only be accessed via [VesselImpl].
 */
@Dao
abstract class VesselDao {
    // region blocking accessors

    /**
     * Get a single entity, by type.
     *
     * @param type qualified name of the data class represented by the enclosed data
     * @return the entity holding the enclosed data, or null if it does not exist
     */
    @Query("SELECT * FROM vessel WHERE type = :type")
    abstract fun getBlocking(type: String): VesselEntity?

    /**
     * Set a single entity.
     *
     * @param entity to store/replace in the database
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun setBlocking(entity: VesselEntity)

    /**
     * Delete a single entity.
     *
     * @param type qualified name of the data class represented by the enclosed data
     */
    @Query("DELETE FROM vessel WHERE type = :type")
    abstract fun deleteBlocking(type: String)

    // endregion

    // region suspend accessors

    /**
     * Get a single entity, by type, in a suspend function.
     *
     * @param type qualified name of the data class represented by the enclosed data
     * @return the entity holding the enclosed data, or null if it does not exist
     */
    @Query("SELECT * FROM vessel WHERE type = :type")
    abstract suspend fun get(type: String): VesselEntity?

    /**
     * Set a single entity, in a suspend function.
     *
     * @param entity to store/replace in the database
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun set(entity: VesselEntity)

    /**
     * Delete a single entity, in a suspend function.
     *
     * @param type qualified name of the data class represented by the enclosed data
     */
    @Query("DELETE FROM vessel WHERE type = :type")
    abstract suspend fun delete(type: String)

    // endregion

    // region observers

    /**
     * Return a flow that monitors when the specified type is updated.
     *
     * @param type qualified name of the data class represented by the enclosed data
     * @return a flow of entities
     */
    @Query("SELECT * FROM vessel WHERE type = :type")
    abstract fun getFlow(type: String): Flow<VesselEntity?>

    /**
     * Return a livedata that monitors when the specified type is updated.
     *
     * @param type qualified name of the data class represented by the enclosed data
     * @return a livedata of entities
     */
    @Query("SELECT * FROM vessel WHERE type = :type")
    abstract fun getLiveData(type: String): LiveData<VesselEntity?>

    // endregion

    // region utilities

    /**
     * Replace an old entity with a new entity inside a single suspending transaction.
     *
     * @param old entity to remove
     * @param new entity to add
     */
    @Transaction
    open suspend fun replace(old: VesselEntity, new: VesselEntity) {
        set(new)
        delete(old.type)
    }

    // endregion
}
