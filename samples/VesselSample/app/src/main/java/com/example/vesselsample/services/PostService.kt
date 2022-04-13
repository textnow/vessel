package com.example.vesselsample.services

import com.example.vesselsample.model.Post
import retrofit2.http.GET
import retrofit2.http.Query

interface PostService {
    @GET("posts")
    suspend fun getPostsForUser(@Query("userId") userId: String): List<Post>
}