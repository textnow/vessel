package com.example.vesselsample.repository

import com.example.vesselsample.model.Stats
import com.textnow.android.vessel.Vessel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface StatsRepository {
    /**
     * [Flow] of [Stats] coming from the local database, or the default
     *  stats if there is none in the database
     */
    fun getStats() : Flow<Stats>

    /**
     * Updates the [Stats] in the local database
     */
    suspend fun updateStats(stats: Stats)
}

class StatsRepositoryImpl : StatsRepository, KoinComponent {
    private val vessel: Vessel by inject()

    override fun getStats(): Flow<Stats> {
        return vessel.flow(Stats::class).map {
            return@map it ?: Stats()
        }
    }

    override suspend fun updateStats(stats: Stats) {
        vessel.set(stats)
    }
}