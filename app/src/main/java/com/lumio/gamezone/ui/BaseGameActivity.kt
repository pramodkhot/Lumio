package com.lumio.gamezone.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.lumio.gamezone.ads.AdManager

abstract class BaseGameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(0xFF080614.toInt())
            )
        }
        window.statusBarColor = 0xFF080614.toInt()
        window.navigationBarColor = 0xFF080614.toInt()
    }

    override fun onResume() {
        super.onResume()
        // Keep AdManager aware of current activity for showing ads
        AdManager.attachActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
