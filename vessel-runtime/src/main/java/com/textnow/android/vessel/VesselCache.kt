package com.textnow.android.vessel

interface VesselCache {
    val size: Int
    suspend fun <T : Any> get(key: String): T?
    suspend fun <T : Any> set(key: String, value: T)
    suspend fun remove(key: String)
    suspend fun clear()
}