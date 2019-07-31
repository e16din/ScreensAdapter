package com.e16din.screensadapter.activities

import android.app.Activity
import android.os.Bundle
import com.e16din.screensadapter.ScreensAdapterApplication

class StarterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
//        window.decorView.apply {
//            // Hide both the navigation bar and the status bar.
//            // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
//            // a general rule, you should design your app to hide the status bar whenever you
//            // hide the navigation bar.
//            systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
//        }
        super.onCreate(savedInstanceState)
        screensAdapter().onStarterActivityCreated(this)
    }

    override fun onPause() {
        overridePendingTransition(0, 0)
        super.onPause()
    }

    private fun screensAdapter() = (application as ScreensAdapterApplication).screensAdapter
}