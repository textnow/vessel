package com.example.vesselsample.services

import com.example.vesselsample.model.User
import retrofit2.http.GET
import retrofit2.http.Path

interface UserService {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): User

    @GET("users")
    suspend fun getAllUsers(): List<User>
}