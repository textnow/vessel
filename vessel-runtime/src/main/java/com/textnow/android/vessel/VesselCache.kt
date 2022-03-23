package com.textnow.android.vessel

interface VesselCache {
    /**
     * The number of items currently in the cache
     */
    val size: Int

    /**
     * Get the value of [key], returning null if [key] is not present in the cache
     */
    fun <T : Any> get(key: String): T?

    /**
     * Set [key] to [value] in the cache
     */
    fun <T : Any> set(key: String, value: T)

    /**
     * Remove [key] from the cache
     */
    fun remove(key: String)

    /**
     * Clear the cache of all data
     */
    fun clear()
}