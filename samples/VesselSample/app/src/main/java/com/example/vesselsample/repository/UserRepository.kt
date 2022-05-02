package com.example.vesselsample.repository

import com.example.vesselsample.model.DeviceInfo
import com.example.vesselsample.model.SessionId
import com.example.vesselsample.model.User
import com.example.vesselsample.services.UserService
import com.example.vesselsample.utils.DispatchProvider
import com.textnow.android.vessel.Vessel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface UserRepository {
    /**
     * Fetches the user by checking cache first,
     * and then fetching from the network in the background
     */
    fun fetchCurrentUser(id: String): Flow<User>

    /**
     * Fetches a user from the API
     */
    suspend fun fetchUser(id: String): User

    /**
     * Fetches all friends for the current user ID
     */
    fun fetchFriends(): Flow<List<User>>

    /**
     * Logs in a user given an ID (1 - 10)
     */
    suspend fun login(userId: String): SessionId

    /**
     * Logs out the current user
     */
    suspend fun logout()

    val userId: Flow<SessionId?>
}

class UserRepositoryImpl(
    private val userService: UserService,
) : UserRepository, KoinComponent {
    private val vessel: Vessel by inject()
    private val dispatchProvider: DispatchProvider by inject()

    override val userId: Flow<SessionId?>
        get() = vessel.flow(SessionId::class)

    // Serve from cache while network request is loading
    override fun fetchCurrentUser(id: String): Flow<User> {
        return flow {
            val cachedUser = vessel.get(User::class)
            if (cachedUser?.id == id) {
                emit(cachedUser)
            }

            val user = userService.getUser(id)
            if (user != cachedUser) {
                vessel.set(user)
                emit(user)
            }

        }.flowOn(dispatchProvider.io())

    }

    override suspend fun fetchUser(id: String): User {
        return userService.getUser(id)
    }

    override fun fetchFriends(): Flow<List<User>> {
        return userId.filterNotNull().map { sessionId ->
            return@map userService.getAllUsers().filter { it.id != sessionId.id  }
        }.flowOn(dispatchProvider.io())
    }

    override suspend fun login(userId: String): SessionId {
        // save the session ID to vessel (for this sample, session ID is user ID)
        val sessionId = SessionId(userId)
        vessel.set(sessionId)
        return sessionId
    }

    override suspend fun logout() {
        // retain the info that needs to be persisted after logout
        val deviceInfo = vessel.get(DeviceInfo::class)
        vessel.clear()
        deviceInfo?.let {
            vessel.set(it)
        }
    }
}