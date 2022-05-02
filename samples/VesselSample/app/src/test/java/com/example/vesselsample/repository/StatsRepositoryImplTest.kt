package com.example.vesselsample.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.vesselsample.model.Stats
import com.example.vesselsample.utils.testDispatchProvider
import com.textnow.android.vessel.Vessel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class StatsRepositoryImplTest : KoinTest {
    private lateinit var statsRepository: StatsRepository
    private val vessel: Vessel = mockk(relaxUnitFun = true)

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single { testDispatchProvider }
                single { vessel }
            })
    }

    @Before
    fun setup() {
        statsRepository = StatsRepositoryImpl()
    }

    @Test
    fun `no stats in vessel returns default`() = runBlocking {
        every { vessel.flow(Stats::class) } returns flowOf(null)
        assert(statsRepository.getStats().first() == Stats())
    }

    @Test
    fun `returns stats from vessel`() = runBlocking {
        val stats = Stats(3f, 4f,2f,1f)
        every { vessel.flow(Stats::class) } returns flowOf(stats)
        assert(statsRepository.getStats().first() == stats)
    }

}