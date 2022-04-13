package com.example.vesselsample

import androidx.lifecycle.*
import com.example.vesselsample.model.DeviceInfo
import com.example.vesselsample.model.SessionId
import com.example.vesselsample.repository.DeviceInfoRepository
import com.example.vesselsample.repository.UserRepository
import com.example.vesselsample.utils.DispatchProvider
import com.example.vesselsample.utils.Response
import com.example.vesselsample.utils.asObservableResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.*

class UserViewModel(
    private val userRepository: UserRepository,
    private val deviceInfoRepository: DeviceInfoRepository
) : ViewModel(), KoinComponent {
    private val dispatchProvider: DispatchProvider by inject()

    private val userId = userRepository.userId.asLiveData(dispatchProvider.io())

    /**
     * Keeps an updated current User object depending
     * on the current signed in user
     */
    val userInfo = userId.switchMap { id ->
        if (id == null) return@switchMap liveData { }
        return@switchMap userRepository.fetchCurrentUser(id.id)
            .asObservableResponse(dispatchProvider.io())
    }

    /**
     * An observable authentication status
     */
    val isAuthenticated = userId.map { it != null }.distinctUntilChanged()

    /**
     * Login the user with [id] and save the ID
     * into local database to remember the session
     */
    fun login(id: String): LiveData<Response<SessionId>> {
        return liveData(dispatchProvider.io()) {
            val sessionId = userRepository.login(id)
            deviceInfoRepository.setDeviceInfo(DeviceInfo(Date().time, id))
            emit(Response.Success(sessionId))
        }
    }

    /**
     * Logout the user and delete all local database info
     */
    fun logout() {
        viewModelScope.launch(dispatchProvider.io()) {
            userRepository.logout()
        }
    }
}