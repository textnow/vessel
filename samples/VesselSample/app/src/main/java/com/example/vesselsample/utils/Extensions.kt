package com.example.vesselsample.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.lang.Exception
import java.net.UnknownHostException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> Flow<T>.asObservableResponse(
    context: CoroutineContext = EmptyCoroutineContext,
    timeoutInMs: Long = 5000L
): LiveData<Response<T>> = liveData(context, timeoutInMs) {
    try {
        emit(Response.Loading)
        collect {
            emit(Response.Success(it))
        }
    } catch (e: Exception) {
        when (e) {
            is UnknownHostException -> emit(Response.Failure("No Internet Connection", e))
            else -> emit(Response.Failure("Oops, Something Went Wrong!", e))
        }
    }
}