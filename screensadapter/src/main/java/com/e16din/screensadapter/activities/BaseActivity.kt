package com.e16din.screensadapter.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.ScreensAdapterApplication
import com.e16din.screensadapter.settings.ScreenSettings

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        const val KEY_SCREEN_SETTINGS = "com.e16din.screensadapter.ScreenSettings"
    }

    private lateinit var settings: ScreenSettings

    private val screensAdapter: ScreensAdapter<*, *>
        get() = (application as ScreensAdapterApplication).screensAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = intent.getParcelableExtra(KEY_SCREEN_SETTINGS) as ScreenSettings

        setTheme(settings.themeId)
        settings.layoutId?.run {
            setContentView(this)
        }

        screensAdapter.onActivityCreated(this)
    }

    override fun onStart() {
        super.onStart()
        screensAdapter.onActivityStart(this)
    }

    override fun onResume() {
        super.onResume()
        screensAdapter.onActivityResume()
    }

    override fun onPause() {
        screensAdapter.onActivityPause()
        super.onPause()
    }

    override fun onStop() {
        screensAdapter.onActivityStop(this)
        super.onStop()
    }

    override fun onBackPressed() {
        screensAdapter.onBackPressed()
    }

    //todo: add onActivityResult()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        settings.menuId?.let {
            menuInflater.inflate(it, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return screensAdapter.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return screensAdapter.onPrepareOptionsMenu(menu)
    }
}