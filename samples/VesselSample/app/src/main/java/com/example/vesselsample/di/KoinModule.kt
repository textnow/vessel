package com.example.vesselsample.di

import com.example.vesselsample.UserViewModel
import com.example.vesselsample.repository.*
import com.example.vesselsample.services.PostService
import com.example.vesselsample.services.UserService
import com.example.vesselsample.ui.auth.LoginViewModel
import com.example.vesselsample.ui.friends.FriendsViewModel
import com.example.vesselsample.ui.main.MainViewModel
import com.example.vesselsample.ui.profile.ProfileViewModel
import com.example.vesselsample.utils.DispatchProvider
import com.textnow.android.vessel.DefaultCache
import com.textnow.android.vessel.Vessel
import com.textnow.android.vessel.VesselCallback
import com.textnow.android.vessel.VesselImpl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

val koinModule = module {
    single<DispatchProvider> { object : DispatchProvider {} }

    single<UserRepository> {
        UserRepositoryImpl(
            userService = get()
        )
    }

    single<DeviceInfoRepository> {
        DeviceInfoRepositoryImpl()
    }

    single<StatsRepository> {
        StatsRepositoryImpl()
    }

    single<PostsRepository> {
        PostsRepositoryImpl(
            postService = get()
        )
    }

    viewModel {
        MainViewModel(
            statsRepository = get()
        )
    }

    viewModel {
        UserViewModel(
            userRepository = get(),
            deviceInfoRepository = get()
        )
    }

    viewModel {
        FriendsViewModel(
            userRepository = get()
        )
    }

    viewModel {
        ProfileViewModel(
            userRepository = get(),
            postsRepository = get()
        )
    }

    viewModel {
        LoginViewModel()
    }

    single<Vessel> {
        VesselImpl(
            appContext = get(),
            name = "vessel-db",
            inMemory = false,
            allowMainThread = false,
            callback = VesselCallback(
                onCreate = { Timber.d("Database created") },
                onOpen = { Timber.d("Database opened") },
                onClosed = { Timber.d("Database closed") },
                onDestructiveMigration = { Timber.d("Destructive migration") }
            ),
            cache = DefaultCache()
        )
    }

    single<Retrofit> {
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<UserService> {
        get<Retrofit>().create(UserService::class.java)
    }

    single<PostService> {
        get<Retrofit>().create(PostService::class.java)
    }
}

