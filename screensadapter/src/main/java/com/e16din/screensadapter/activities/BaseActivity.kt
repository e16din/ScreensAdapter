package com.e16din.screensadapter.activities

import android.os.Bundle
import android.view.Menu
import android.view.View
import com.e16din.screensadapter.ScreenSettings
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.activities.richactivity.RichCompatActivity
import com.e16din.screensadapter.helpers.ActivityUtils
import com.e16din.screensadapter.helpers.addListener


abstract class BaseActivity : RichCompatActivity() {

    companion object {
        var symbiontListener: ((activity: BaseActivity) -> Unit)? = null
    }

    init {
        initListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        symbiontListener?.invoke(this)
        super.onCreate(savedInstanceState)
    }

    private lateinit var settings: ScreenSettings

    private fun initListeners() {
        onCreateBeforeSuperCallEvent.addListener {
            settings = ScreensAdapter.get.items.last()

            if (settings.isTranslucent) {
                ActivityUtils.convertActivityToTranslucent(this@BaseActivity)
            }

            if (settings.isFullscreen || settings.isDialog) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
            }

            if (!settings.isDialog && !settings.isTranslucent) {
                setTheme(settings.themeId)
            }

            requestedOrientation = settings.orientation
        }

        onCreateEvent.addListener {
            settings.layoutId?.run {
                setContentView(this)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        settings.menuId?.let {
            menuInflater.inflate(it, menu)
        }
        return true
    }
}