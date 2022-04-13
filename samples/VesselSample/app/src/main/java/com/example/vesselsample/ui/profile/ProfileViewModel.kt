package com.example.vesselsample.ui.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.example.vesselsample.repository.PostsRepository
import com.example.vesselsample.repository.UserRepository
import com.example.vesselsample.utils.DispatchProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val postsRepository: PostsRepository
) : ViewModel(), KoinComponent {
    private val dispatchProvider: DispatchProvider by inject()

    /**
     * User ID of the current profile
     */
    private val _userID = MutableLiveData<String>()

    /**
     * User info of the current profile
     */
    val userInfo = _userID.switchMap {
        return@switchMap liveData(dispatchProvider.io()) {
            emit(userRepository.fetchUser(it))
        }
    }

    /**
     * The posts of the user whose profile is being viewed
     */
    val userPosts = _userID.switchMap {
        return@switchMap liveData(dispatchProvider.io()) {
            emit(postsRepository.getPostsForUser(it))
        }
    }

    /**
     * Set which user's profile is being viewed
     */
    fun setUserID(id: String) {
        _userID.value = id
    }
}