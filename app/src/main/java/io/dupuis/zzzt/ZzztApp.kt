package io.dupuis.zzzt

import android.app.Application
import io.dupuis.zzzt.di.AppContainer

class ZzztApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
