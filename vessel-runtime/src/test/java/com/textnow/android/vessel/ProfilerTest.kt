package com.textnow.android.vessel

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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isTrue
import assertk.assertions.matches
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.koin.test.KoinTestRule
import kotlin.random.Random

/**
 * Validate ability to mock Vessel when an in-memory db is not desired.
 */
@RunWith(JUnit4::class)
class ProfilerTest {
    private val profiler = ProfilerImpl()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        printLogger()
    }

    @Test
    fun `profiler captures span count and duration`() {
        profiler.timeBlocking(Span.READ_FROM_DB) {
            Thread.sleep(100)
        }

        val profData = profiler.snapshot

        assertThat(profData.hitCountOf(Span.READ_FROM_DB)).isEqualTo(1)
        assertThat(profData.timeIn(Span.READ_FROM_DB)).isGreaterThanOrEqualTo(100)
    }

    @Test
    fun `async profiler captures span count and duration`() {
        runBlocking {
            profiler.time(Span.READ_FROM_DB) {
                delay(100)
            }
        }

        val profData = profiler.snapshot

        assertThat(profData.hitCountOf(Span.READ_FROM_DB)).isEqualTo(1)
        assertThat(profData.timeIn(Span.READ_FROM_DB)).isGreaterThanOrEqualTo(100)
    }

    @Test
    fun `profiler captures event count`() {
        profiler.countBlocking(Event.CACHE_HIT_READ)

        val profData = profiler.snapshot

        assertThat(profData.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(1)
    }

    @Test
    fun `async profiler captures event count`() {
        runBlocking {
            profiler.count(Event.CACHE_HIT_READ)
        }

        val profData = profiler.snapshot

        assertThat(profData.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(1)
    }

    @Test
    fun `profiler sums spans`() {
        profiler.timeBlocking(Span.READ_FROM_DB) {
            Thread.sleep(10)
        }
        profiler.timeBlocking(Span.READ_FROM_DB) {
            Thread.sleep(10)
        }

        val profData = profiler.snapshot

        assertThat(profData.hitCountOf(Span.READ_FROM_DB)).isEqualTo(2)
        assertThat(profData.timeIn(Span.READ_FROM_DB)).isGreaterThanOrEqualTo(20)
    }

    @Test
    fun `profiler sums events`() {
        profiler.countBlocking(Event.CACHE_HIT_READ)
        profiler.countBlocking(Event.CACHE_HIT_READ)

        val profData = profiler.snapshot

        assertThat(profData.hitCountOf(Event.CACHE_HIT_READ)).isEqualTo(2)
    }

    @Test
    fun `summary text is sound`() {
        val threadCount = 5

        val invokeManyOf = { block: () -> Unit ->
            for (i in 0..3) {
                block()
            }
        }

        (1..threadCount).map {
            Thread({
                Event.values().forEach {
                    invokeManyOf { profiler.countBlocking(it) }
                }
                Span.values().forEach {
                    invokeManyOf {
                        profiler.timeBlocking(it) { Thread.sleep(10) }
                    }
                }
            }, "test-$it").apply { start() }
        }.forEach { it.join(100) }

        fun row(repeat: Int, vararg words: String) =
            (0 until repeat).joinToString("\n") { "\\s*${words.joinToString("\\s+│\\s+")}\\s*" }

        val separatorRow =
            { columns: Int, line: String, divider: String -> (1..columns).joinToString(divider) { "${line}+" } }
        val word = "[^\\s]+"
        val number = "\\d+"
        val space = "\\s+"
        val repeat = { count: Int, value: String -> (1..count).map { value }.toTypedArray() }

        val pattern = """Database I/O times, sorted by time spent
${row(1, "span", "count", "time")}
${separatorRow(3, "═", "╪")}
${row(Span.values().count(), word, number, number)}
${separatorRow(3, "─", "┼")}
${row(1, space, number, number)}

Cache hits, sorted by hit count
${row(1, "event", "count")}
${separatorRow(2, "═", "╪")}
${(row(Event.values().count(), word, number))}
${separatorRow(2, "─", "┼")}
${row(1, space, number)}

Database I/O times, by thread/coroutine, sorted by time spent
${row(1, "type", "name", *repeat(7, word))}
${separatorRow(9, "═", "╪")}
${row(threadCount, word, word, *repeat(7, number))}
${separatorRow(9, "─", "┼")}
${row(1, space, space, *repeat(7, number))}

\* all times in ms
"""

        assertThat(profiler.snapshot.summary).matches(pattern.toRegex())
    }

}
