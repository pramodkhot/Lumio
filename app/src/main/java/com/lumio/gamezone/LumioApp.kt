package com.lumio.gamezone

import android.app.Application
import com.facebook.ads.AudienceNetworkAds

/**
 * Application class — initialises Facebook Audience Network SDK
 * as early as possible for fastest ad loading.
 */
class LumioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialise FAN SDK on app start
        AudienceNetworkAds.initialize(this)
    }
}
