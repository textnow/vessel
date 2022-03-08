package com.textnow.android.vessel

interface VesselCache {
    /**
     * Get the current size of the cache
     */
    val size: Int

    /**
     * Get the value of [key], returning null if key is not present
     */
    fun <T : Any> get(key: String): T?

    /**
     * Sets [key] to [value] in the cache
     */
    fun <T : Any> set(key: String, value: T)

    /**
     * Removes [key] from the cache
     */
    fun remove(key: String)

    /**
     * Clears the cache
     */
    fun clear()
}