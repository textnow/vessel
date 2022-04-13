package com.example.vesselsample.repository

import com.example.vesselsample.model.Post
import com.example.vesselsample.services.PostService
import com.example.vesselsample.utils.testDispatchProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@RunWith(JUnit4::class)
@ExperimentalCoroutinesApi
class PostsRepositoryImplTest : KoinTest {
    private lateinit var postsRepository: PostsRepository
    private val postsService: PostService = mockk(relaxUnitFun = true)

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single { testDispatchProvider }
                single { postsService }
            })
    }

    @Before
    fun setup() {
        postsRepository = PostsRepositoryImpl(get())
    }

    @Test
    fun `fetch posts is called`() = runBlocking {
        val posts = listOf(
            Post("1", "test", "test title", "test description"),
            Post("2", "test", "test title 2", "test description 2")
        )
        coEvery { postsService.getPostsForUser("test") } returns posts
        val receivedPosts = postsRepository.getPostsForUser("test")
        assert(posts == receivedPosts)

    }
}



