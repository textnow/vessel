package com.example.vesselsample.repository

import com.example.vesselsample.model.DeviceInfo
import com.textnow.android.vessel.Vessel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface DeviceInfoRepository {
    /**
     * Sets the [deviceInfo] into the local database. This includes details about the user's
     * last log in date and the last logged in ID
     */
    suspend fun setDeviceInfo(deviceInfo: DeviceInfo)

    /**
     * Retrieve the local device info from the local database
     */
    suspend fun getDeviceInfo(): DeviceInfo?
}

class DeviceInfoRepositoryImpl: DeviceInfoRepository, KoinComponent {
    private val vessel: Vessel by inject()

    override suspend fun setDeviceInfo(deviceInfo: DeviceInfo) {
        vessel.set(deviceInfo)
    }

    override suspend fun getDeviceInfo(): DeviceInfo? {
        return vessel.get(DeviceInfo::class)
    }

}