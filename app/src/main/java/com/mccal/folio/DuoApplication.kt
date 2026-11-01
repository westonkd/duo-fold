package com.mccal.folio

import android.app.Application

class DuoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        restoreLauncherEntry(this)
    }
}

