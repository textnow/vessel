package com.example.vesselsample.utils

/**
 * Helper class to associate a status
 * with an event
 */
sealed class Response<out T> {
    data class Success<out R>(val value: R) : Response<R>()
    data class Failure(val message: String?, val throwable: Throwable? = null) : Response<Nothing>()
    object Loading : Response<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[value=$value]"
            is Failure -> "Error[exception=$throwable]"
            Loading -> "Loading"
        }
    }
}

inline fun <reified T> Response<T>.doWhileLoading(callback: () -> Unit) {
    if (this is Response.Loading) {
        callback()
    }
}

inline fun <reified T> Response<T>.doIfFailure(callback: (error: String?, throwable: Throwable?) -> Unit) {
    if (this is Response.Failure) {
        callback(message, throwable)
    }
}

inline fun <reified T> Response<T>.doIfSuccess(callback: (value: T) -> Unit) {
    if (this is Response.Success) {
        callback(value)
    }
}