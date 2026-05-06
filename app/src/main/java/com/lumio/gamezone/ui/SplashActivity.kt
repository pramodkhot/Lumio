package com.lumio.gamezone.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.lumio.gamezone.R
import com.lumio.gamezone.ads.AdManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        window.statusBarColor = 0xFF080614.toInt()
        window.navigationBarColor = 0xFF080614.toInt()

        // Initialise Ad Manager — loads first interstitial in background
        AdManager.init(this)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}
