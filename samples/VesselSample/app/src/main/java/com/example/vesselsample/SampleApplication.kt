package com.example.vesselsample

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import com.example.vesselsample.di.koinModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import timber.log.Timber.*


class SampleApplication : Application() {
    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build()
            )
//            StrictMode.setVmPolicy(
//                VmPolicy.Builder()
//                    .detectLeakedSqlLiteObjects()
//                    .detectLeakedClosableObjects()
//                    .detectActivityLeaks()
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build()
//            )
        }

        super.onCreate()

        // Set up logging
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        setUpDependencies()
    }

    private fun setUpDependencies() {
        // start koin
        startKoin {
            androidContext(this@SampleApplication)

            modules(
                koinModule
            )
        }
    }

    private class CrashReportingTree : Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }
            // TODO: include the following lines to log crashes through a library (such as Embrace)
//            Embrace.log(priority, tag, message)
//            t?.let {
//                when (priority) {
//                    Log.ERROR -> Embrace.logError(t)
//                    Log.WARN -> Embrace.logWarning(t)
//                    else -> return@let
//                }
//            }
        }
    }
}