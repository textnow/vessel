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

import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlin.system.measureTimeMillis

/**
 * A time span over which some database operation occurred.
 * Used in profiling database operations when using a [Vessel] instance.
 * This shows where time is being spent when using [Vessel]
 */
enum class Span {
    READ_FROM_DB,
    WRITE_TO_DB,
    PRELOAD_FROM_DB,
    DELETE_FROM_DB,
    REPLACE_IN_DB,
    CLEAR_DB;

    /**
     * Friendly name for each enum - ex, "read", "write", etc.
     */
    fun niceName(): String {
        return this.name.substring(0, this.name.indexOf("_")).lowercase()
    }
}

/**
 * A significant event that occurred when using a [Vessel] instance.
 * Used to profile important events such as cache hits.
 */
enum class Event {
    CACHE_HIT_READ,
    CACHE_HIT_WRITE,
    CACHE_HIT_DELETE,
    CACHE_HIT_REPLACE;

    /**
     * Friendly name for each enum - ex, "read", "delete", etc.
     */
    fun niceName(): String {
        return this.name.substring(this.name.lastIndexOf("_") + 1).lowercase()
    }
}

/**
 * A [Worker] is one of either a thread or a coroutine, and indicates where work is being done
 */
enum class WorkerType {
    COROUTINE,
    THREAD,
}

/**
 * Tracks a worker - its [WorkerType] and name (thread or coroutine name)
 */
data class Worker(val type: WorkerType, val name: String)

/**
 * Tracks data for one [Span] - how many times it was entered, and how much time (total) has been
 * spent inside this span
 */
data class SpanData(val span: Span, var hitCount: Int = 0, var totalDurationMS: Long = 0L)

/**
 * Tracks data for one [Event] - how many times the event has been hit
 */
data class EventData(val event: Event, var hitCount: Int = 0)

/**
 * Consolidate all profiling data for one [Worker] - the [Span]s and [Event]s executed by this
 * [Worker].  In short, where this [Worker] is spending its time when using [Vessel].
 */
data class WorkerData(
    val worker: Worker,
    val spans: MutableMap<Span, SpanData> = mutableMapOf(),
    val events: MutableMap<Event, EventData> = mutableMapOf(),
) {
    /**
     * Total time spent by this [Worker], across all [Span]s
     * That is, the time spent across all operations performed by this [Worker] in a [Vessel] instance
     */
    val totalDurationMS get() = spans.values.sumOf { it.totalDurationMS }
}

/**
 * Helper to print text tables
 */
private fun table(
    headings: List<Any>,
    data: List<List<Any>>,
    summaryRow: List<Any>? = null
): String {
    val allData = data + (summaryRow?.let { listOf(it) } ?: emptyList())
    val headingsAndData = listOf(headings) + allData

    // max number of columns in table (some rows may be sparse)
    val width = headingsAndData.maxOf { row -> row.size }

    // find max width of each column in table
    val colWidths = (0 until width).map { colNo ->
        headingsAndData.maxOf { row ->
            row.getOrNull(colNo)?.toString()?.length ?: 0
        }
    }

    fun List<Any>.toTextRow() =
        this.mapIndexed { colNo, item -> " ${item.toString().padStart(colWidths[colNo])} " }
            .joinToString("│")

    val separatorRow = { line: String, divider: String ->
        colWidths.joinToString(divider) { colWidth ->
            line.repeat(colWidth + 2)
        }
    }

    // headings, divider row, data rows, divider row, summary row
    return listOfNotNull(
        headings.toTextRow(),
        separatorRow("═", "╪"),
        *data.map { it.toTextRow() }.toTypedArray(),
        separatorRow("─", "┼"),
        summaryRow?.toTextRow(),
    ).joinToString("\n")
}

/**
 * All profiling data for all [Worker]s
 */
data class ProfileData(val _workerData: Set<WorkerData>) {

    /**
     * Returns the time spent across all [Worker]s in the given [Span]
     */
    fun timeIn(span: Span) = workerData.sumOf { it.spans[span]?.totalDurationMS ?: 0 }

    /**
     * Returns the number of time the given [Span] was entered, across all [Worker]s
     */
    fun hitCountOf(span: Span) = workerData.sumOf { it.spans[span]?.hitCount ?: 0 }

    /**
     * Returns the number of time the given [Event] was entered, across all [Worker]s
     */
    fun hitCountOf(event: Event) = workerData.sumOf { it.events[event]?.hitCount ?: 0 }

    /**
     * All [Worker]s, sorted by total time spent in each (descending)
     */
    val workerData by lazy { _workerData.sortedByDescending { it.totalDurationMS } }

    /**
     * All [Span]s, sorted by total time in each (descending)
     */
    val spans by lazy { Span.values().sortedByDescending { timeIn(it) } }

    /**
     * All [Event]s, sorted by hit count (descending)
     */
    val events by lazy { Event.values().sortedByDescending { hitCountOf(it) } }

    /**
     * Generates a textual summary of this profile data.
     * Useful for manual inspection
     */
    val summary by lazy {
        """Database I/O times, sorted by time spent
${
            table(
                headings = listOf("span", "count", "time"),
                data = spans.map { listOf(it.niceName(), hitCountOf(it), timeIn(it)) },
                summaryRow = listOf("", spans.sumOf { hitCountOf(it) }, spans.sumOf { timeIn(it) })
            )
        }

Cache hits, sorted by hit count
${
            table(
                headings = listOf("event", "count"),
                data = events.map { listOf(it.niceName(), hitCountOf(it)) },
                summaryRow = listOf("", events.sumOf { hitCountOf(it) })
            )
        }

Database I/O times, by thread/coroutine, sorted by time spent
${
            table(
                headings = listOf("type", "name") + spans.map { it.niceName() } + listOf("total"),
                data = workerData.map {
                    listOf(
                        it.worker.type,
                        it.worker.name
                    ) + spans.map { span -> it.spans[span]?.totalDurationMS ?: 0 } + listOf(
                        it.totalDurationMS
                    )
                },
                summaryRow = listOf(
                    "", /* type */
                    "", /* name */
                ) + spans.map { timeIn(it) } + spans.sumOf { timeIn(it) }

            )
        }

* all times in ms
"""
    }
}

/**
 * Profiler for use by [Vessel]
 * Can be used to tracks time spent performing database operations by worker (thread/coroutine), and
 * to count cache hits
 */
internal interface Profiler {
    /**
     * Track execution time for [block]
     * This is added to the running total of spent in the given [span] for the current coroutine
     */
    suspend fun <T> time(span: Span, block: suspend () -> T): T

    /**
     * Track execution time for [block]
     * This is added to the running total of time spent in the given [span] for the current thread
     */
    fun <T> timeBlocking(span: Span, block: () -> T): T

    /**
     * Increment the hit count for the given event
     * This is added to the running total of hits for given [event], for the current coroutine
     */
    suspend fun count(event: Event)

    /**
     * Increment the hit count for the given event
     * This is added to the running total of hits for given [event], for the current thread
     */
    fun countBlocking(event: Event)

    /**
     * Retrieve a copy of the current [ProfileData]
     * Note that this is a copy and will not reflect future changes
     */
    val snapshot: ProfileData
}

internal class DummyProfiler : Profiler {
    override suspend fun <T> time(span: Span, block: suspend () -> T): T {
        return block()
    }

    override fun <T> timeBlocking(span: Span, block: () -> T): T {
        return block()
    }

    override suspend fun count(event: Event) {
    }

    override fun countBlocking(event: Event) {
    }

    override val snapshot get() = ProfileData(emptySet())

}

internal class ProfilerImpl : Profiler {
    private val workers = ConcurrentHashMap<Worker, WorkerData>()

    private fun timeImpl(worker: Worker, span: Span, duration: Long) {
        val workerData = workers.getOrPut(worker) {
            WorkerData(worker)
        }

        // Note:  synchronized is safe - and required - here:
        // * it is a short critical section, so will not block other threads/coroutines
        // * it is fine-grained, so will not block others accessing other data in this function
        // * it does not contain any suspension points (suspend fun calls)
        // Without this, workerData or spanData could become corrupted
        synchronized(workerData) {
            val spanData = workerData.spans.getOrPut(span) { SpanData(span) }
            spanData.totalDurationMS += duration
            spanData.hitCount++
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun <T> time(span: Span, block: suspend () -> T): T {
        val worker =
            Worker(WorkerType.COROUTINE, coroutineContext[CoroutineDispatcher].toString())

        val retVal: T
        val duration = measureTimeMillis { retVal = block() }

        timeImpl(worker, span, duration)

        return retVal
    }

    override fun <T> timeBlocking(span: Span, block: () -> T): T {
        val worker = Worker(WorkerType.THREAD, Thread.currentThread().name)

        val retVal: T
        val duration = measureTimeMillis { retVal = block() }

        timeImpl(worker, span, duration)

        return retVal
    }

    private fun countImpl(worker: Worker, event: Event) {
        val workerData = workers.getOrPut(worker) {
            WorkerData(worker)
        }

        synchronized(workerData) {
            workerData.events.getOrPut(event) { EventData(event) }.hitCount++
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun count(event: Event) {
        val worker =
            Worker(WorkerType.COROUTINE, coroutineContext[CoroutineDispatcher].toString())
        countImpl(worker, event)
    }

    override fun countBlocking(event: Event) {
        val worker = Worker(WorkerType.THREAD, Thread.currentThread().name)
        countImpl(worker, event)
    }

    override val snapshot
        get() = ProfileData(workers.values.map { synchronized(it) { it.copy() } }.toSet())
}