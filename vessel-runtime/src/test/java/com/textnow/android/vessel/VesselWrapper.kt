package com.textnow.android.vessel

import kotlin.reflect.KClass

/**
 * Wraps the basic Vessel get/set/delete operations, so this test can reuse the same
 * code to test the blocking and suspend functions
 */
class VesselWrapper(private val vessel: Vessel, private val async: Boolean = false) {
    suspend fun <T: Any> get(type: KClass<T>): T? {
        return if (async) {
            vessel.get(type)
        } else {
            vessel.getBlocking(type)
        }
    }

    suspend fun set(value: Any) {
        if (async) {
            vessel.set(value)
        } else {
            vessel.setBlocking(value)
        }
    }

    suspend fun <T: Any> delete(type: KClass<T>) {
        if (async) {
            vessel.delete(type)
        } else {
            vessel.deleteBlocking(type)
        }
    }

    suspend fun preload(timeoutMS: Int?) {
        if (async) {
            vessel.preload(timeoutMS)
        } else {
            vessel.preloadBlocking(timeoutMS)
        }
    }

    val profileData get() = vessel.profileData
}