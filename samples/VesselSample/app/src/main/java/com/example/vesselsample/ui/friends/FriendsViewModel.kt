package com.example.vesselsample.ui.friends

import androidx.lifecycle.ViewModel
import com.example.vesselsample.repository.UserRepository
import com.example.vesselsample.utils.DispatchProvider
import com.example.vesselsample.utils.asObservableResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FriendsViewModel(
    userRepository: UserRepository
) : ViewModel(), KoinComponent {
    private val dispatchProvider: DispatchProvider by inject()
    val friends =
        userRepository.fetchFriends()
            .asObservableResponse(context = dispatchProvider.io(), timeoutInMs = 60000L)
}