package com.example.vesselsample

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.vesselsample.model.SessionId
import com.example.vesselsample.repository.DeviceInfoRepository
import com.example.vesselsample.repository.UserRepository
import com.example.vesselsample.utils.Response
import com.example.vesselsample.utils.testDispatchProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class UserViewModelTest: KoinTest {
    private val userRepository: UserRepository = mockk(relaxUnitFun = true)
    private val deviceInfoRepository: DeviceInfoRepository = mockk(relaxUnitFun = true)
    private lateinit var viewModel: UserViewModel

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single { userRepository }
                single { testDispatchProvider }
                single { deviceInfoRepository }
            })
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatchProvider.main())
        coEvery { userRepository.userId } returns flowOf(null)
        viewModel = UserViewModel(get(), get())
    }

    @Test
    fun `test login is called`() {
        coEvery { userRepository.login("test") } returns SessionId("test")
        viewModel.login("test").observeForever {
            assert(it is Response.Success && it.value == SessionId("test"))
        }
        coVerify(exactly = 1) { userRepository.login("test") }
    }

    @Test
    fun `test logout called`() {
        viewModel.logout()
        coVerify(exactly = 1) { userRepository.logout() }
    }

    @Test
    fun `fetch user info when userID is changed`() {
    }

}