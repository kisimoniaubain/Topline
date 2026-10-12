package com.kisimoni.topline

import android.app.Application
import com.kisimoni.topline.data.local.UserPreferences

class ToplineApplication : Application() {
    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(this)
    }
}
