package com.example.bpscnotes

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BpscApplication : Application() {
    // AdManager is injected as a singleton — initialize it here so ads
    // are pre-loaded before the user reaches any screen with ads.
    @javax.inject.Inject lateinit var adManager: com.example.bpscnotes.core.ads.AdManager

    override fun onCreate() {
        super.onCreate()
        adManager.initialize()  // Init AdMob SDK + pre-load rewarded & interstitial ads
    }
}