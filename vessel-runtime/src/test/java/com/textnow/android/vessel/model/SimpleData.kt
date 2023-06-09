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
package com.textnow.android.vessel.model

import java.util.*

/**
 * Model for basic serialization tests.
 */
data class SimpleData(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val number: Int?
)

/**
 * An instance of [SimpleData] for testing.
 */
val firstSimple = SimpleData(
    name = "Test One",
    number = 1)

/**
 * Another instance of [SimpleData] for testing.
 */
val secondSimple = SimpleData(
    name = "Test Two",
    number = 2)

/**
 * Model for replacement serialization tests.
 */
data class SimpleDataV2(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val number: Double?
)

val firstSimpleV2 = SimpleDataV2(
    name = "Test One V2",
    number = 1.2
)