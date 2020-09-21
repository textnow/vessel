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

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Provide callback notification when certain database events have occurred.
 *
 * @param onCreate optional lambda called when the database has been created
 * @param onOpen optional lambda called when the database has been opened
 * @param onClosed optional lambda called when the database has been closed
 * @param onDestructiveMigration optional lambda called when the database has been migrated destructively
 * @see RoomDatabase.Callback
 */
class VesselCallback(
        val onCreate: ((SupportSQLiteDatabase) -> Unit)? = null,
        val onOpen: ((SupportSQLiteDatabase) -> Unit)? = null,
        val onClosed: (() -> Unit)? = null,
        val onDestructiveMigration: ((SupportSQLiteDatabase) -> Unit)? = null
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) { onCreate?.invoke(db) }
    override fun onOpen(db: SupportSQLiteDatabase) { onOpen?.invoke(db) }
    override fun onDestructiveMigration(db: SupportSQLiteDatabase) { onDestructiveMigration?.invoke(db) }
}
