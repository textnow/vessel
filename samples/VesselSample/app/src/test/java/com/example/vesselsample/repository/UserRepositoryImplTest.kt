package com.example.vesselsample.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.vesselsample.model.DeviceInfo
import com.example.vesselsample.model.SessionId
import com.example.vesselsample.model.User
import com.example.vesselsample.services.UserService
import com.example.vesselsample.utils.testDispatchProvider
import com.textnow.android.vessel.Vessel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
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
class UserRepositoryImplTest: KoinTest {

    private val userDataSource: UserService = mockk(relaxUnitFun = true)
    private lateinit var userRepository: UserRepository
    private val vessel: Vessel = mockk(relaxUnitFun = true)

    private val fakeUser = User("test", "testName", "username", "email", "phone")

    @get:Rule
    val taskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single { userDataSource }
                single { testDispatchProvider }
                single { vessel }
            })
    }

    @Before
    fun setup() {
        userRepository = UserRepositoryImpl(get())
    }

    @Test
    fun `fetch user no cache`() = runBlocking {
        coEvery { vessel.get(User::class) } returns null
        coEvery { userDataSource.getUser("test") } returns fakeUser
        val fetchedValues = userRepository.fetchCurrentUser("test").toList()
        assert(fetchedValues.size == 1)
        assert(fetchedValues.first() == fakeUser)
        return@runBlocking
    }

    @Test
    fun `fetch user but cache and remote differ`() = runBlocking {
        val fakeUserUpdated = User("test", "updated", "test", "email", "phone")
        coEvery { vessel.get(User::class) } returns fakeUser
        coEvery { userDataSource.getUser("test") } returns fakeUserUpdated
        val fetchedUsers = userRepository.fetchCurrentUser("test").toList()
        coVerify(exactly = 1) { vessel.set(fakeUserUpdated) }
        assert(fetchedUsers.size == 2)
        assert(fetchedUsers.first() == fakeUser)
        assert(fetchedUsers[1] == fakeUserUpdated)
        return@runBlocking
    }

    @Test
    fun `fetch user but cache and remote are same`()  = runBlocking {
        coEvery { vessel.get(User::class) } returns fakeUser
        coEvery { userDataSource.getUser("test") } returns fakeUser
        val fetchedValues = userRepository.fetchCurrentUser("test").toList()
        coVerify(inverse = true) { vessel.set(fakeUser) }
        assert(fetchedValues.size == 1)
        assert(fetchedValues.first() == fakeUser)
        return@runBlocking
    }


    @Test
    fun `fetch friends for user ID`() = runBlocking {
        every { vessel.flow(SessionId::class) } returns flowOf(SessionId("test"))

        val fakeUser2 = User("test2", "updated", "test", "email", "phone")
        coEvery { userDataSource.getAllUsers() } returns listOf(fakeUser, fakeUser2)
        val fetchedValues = userRepository.fetchFriends().toList()
        assert(fetchedValues.size == 1)
        val friends = fetchedValues.first()
        assert(friends.size == 1)
        assert(friends.first() == fakeUser2)
    }

    @Test
    fun `login user`() = runBlocking {
        val expectedSessionId = SessionId("test")
        val actualSessionId = userRepository.login("test")
        coVerify(exactly = 1) { vessel.set(expectedSessionId) }
        assert(expectedSessionId == actualSessionId)
    }

    @Test
    fun `logout calls clear vessel`() = runBlocking {
        coEvery { vessel.get(DeviceInfo::class) } returns DeviceInfo(0L, "1")
        userRepository.logout()
        verify(exactly = 1) { vessel.clear() }
    }
}