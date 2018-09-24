package com.e16din.screensadapter.activities

import android.app.Activity
import android.os.Bundle
import com.e16din.screensadapter.ScreensAdapterApplication

class StarterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screensAdapter().onStarterActivityCreated(this)
    }

    override fun onPause() {
        overridePendingTransition(0, 0)
        super.onPause()
    }

    private fun screensAdapter() = (application as ScreensAdapterApplication).screensAdapter
}