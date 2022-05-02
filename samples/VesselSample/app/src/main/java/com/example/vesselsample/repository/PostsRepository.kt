package com.example.vesselsample.repository

import com.example.vesselsample.model.Post
import com.example.vesselsample.services.PostService

interface PostsRepository {
    /**
     * Retrieves all posts for a given user by their [userId]
     */
    suspend fun getPostsForUser(userId: String) : List<Post>
}

class PostsRepositoryImpl(private val postService: PostService): PostsRepository {
    override suspend fun getPostsForUser(userId: String) : List<Post> {
        return postService.getPostsForUser(userId)
    }
}