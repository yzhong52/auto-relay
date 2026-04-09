package com.autorelay.app

import android.app.Application
import com.autorelay.app.data.RelayLog

class AutoRelayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RelayLog.init(this)
    }
}
