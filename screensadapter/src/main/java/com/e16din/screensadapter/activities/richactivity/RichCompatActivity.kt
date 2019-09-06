package com.e16din.screensadapter.activities.richactivity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.e16din.screensadapter.helpers.EventHolder


abstract class RichCompatActivity : AppCompatActivity() {

    var onCreateBeforeSuperCallEvent = EventHolder<Bundle, Unit>()
    var onCreateEvent = EventHolder<Bundle, Unit>()
    var onPostCreateEvent = EventHolder<Bundle, Unit>()
    var onStartEvent = EventHolder<Nothing, Unit>()
    var onResumeEvent = EventHolder<Nothing, Unit>()
    var onStopEvent = EventHolder<Nothing, Unit>()
    var onPauseEvent = EventHolder<Nothing, Unit>()
    var onBackPressedEvent = EventHolder<Nothing, Unit>()

    var onSaveInstanceStateEvent = EventHolder<Bundle, Unit>()

    data class ActivityResultData(val requestCode: Int, val resultCode: Int, val data: Intent?)

    var onActivityResultEvent = EventHolder<ActivityResultData, Unit>()

    data class RequestPermissionsResultData(val requestCode: Int, val permissions: Array<out String>, val grantResults: IntArray)

    var onRequestPermissionsResultEvent = EventHolder<RequestPermissionsResultData, Unit>()

    var onPrepareOptionsMenuEvent = EventHolder<Menu, Boolean>()
    var onOptionsItemSelectedEvent = EventHolder<MenuItem, Boolean>()


    override fun onCreate(savedInstanceState: Bundle?) {
        onCreateBeforeSuperCallEvent.invoke(savedInstanceState)
        super.onCreate(savedInstanceState)
        onCreateEvent.invoke(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        onStartEvent.invoke()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        onPostCreateEvent.invoke(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        onResumeEvent.invoke()
    }

    override fun onPause() {
        onPauseEvent.invoke()
        super.onPause()
    }

    override fun onStop() {
        onStopEvent.invoke()
        super.onStop()
    }

    override fun onBackPressed() {
        onBackPressedEvent.invoke()
                ?: super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        onSaveInstanceStateEvent.invoke(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        onActivityResultEvent.invoke(ActivityResultData(requestCode, resultCode, data))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResultEvent.invoke(RequestPermissionsResultData(requestCode, permissions, grantResults))
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return onPrepareOptionsMenuEvent.invoke(menu) ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return onOptionsItemSelectedEvent.invoke(item) ?: false
    }
}