package com.example.vesselsample.ui.main

import androidx.lifecycle.*
import com.example.vesselsample.model.SessionId
import com.example.vesselsample.model.Stats
import com.example.vesselsample.repository.StatsRepository
import com.example.vesselsample.repository.UserRepository
import com.example.vesselsample.utils.DispatchProvider
import com.example.vesselsample.utils.Response
import com.example.vesselsample.utils.asObservableResponse
import com.textnow.android.vessel.Vessel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainViewModel(
    private val statsRepository: StatsRepository
) : ViewModel(), KoinComponent {
    private val dispatchProvider: DispatchProvider by inject()
    val stats = statsRepository.getStats().asObservableResponse()

    /**
     * Used to only display the animations once in MainFragment
     */
    var isFirstLoad = true

    fun updateStats(update: (Stats) -> Stats) {
        (stats.value as? Response.Success)?.value?.let { oldStats ->
            viewModelScope.launch(dispatchProvider.io()) {
                statsRepository.updateStats(stats = update(oldStats))
            }
        }
    }
}