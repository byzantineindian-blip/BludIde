package com.bludosmodding

import android.app.Application
import com.google.android.material.color.DynamicColors

class BludIdeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic colors to all activities in the app
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
