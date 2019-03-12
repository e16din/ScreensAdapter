package com.e16din.screensadapter.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.ScreensAdapterApplication
import com.e16din.screensadapter.settings.ScreenSettings

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var settings: ScreenSettings

    private val screensAdapter: ScreensAdapter<*, *>
        get() = (application as ScreensAdapterApplication).screensAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = screensAdapter.getCurrentSettings()

        setTheme(settings.themeId)
        settings.layoutId?.run {
            setContentView(this)
        }

        screensAdapter.onActivityCreated(this, settings.screenCls)
    }

    override fun onStart() {
        super.onStart()
        screensAdapter.onActivityStart(this, settings.screenCls)
    }

    override fun onResume() {
        super.onResume()
        screensAdapter.onActivityResume(settings.screenCls)
    }

    override fun onPause() {
        screensAdapter.onActivityPause(settings.screenCls)
        super.onPause()
    }

    override fun onStop() {
        screensAdapter.onActivityStop(this, settings.screenCls)
        super.onStop()
    }

    override fun onBackPressed() {
        screensAdapter.onBackPressed()
    }

    fun superOnBackPressed() {
        super.onBackPressed()
    }

    //todo: add onActivityResult()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
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