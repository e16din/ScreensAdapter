package com.e16din.screensadapter.activities.richactivity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity


abstract class RichCompatActivity : AppCompatActivity() {

    var events = ActivityEvents()

    fun resetAll() {
        events = ActivityEvents()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        events.onCreateBeforeSuperCallEvent.invoke(savedInstanceState)
        super.onCreate(savedInstanceState)
        events.onCreateEvent.invoke(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        events.onStartEvent.invoke()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        events.onPostCreateEvent.invoke(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        events.onResumeEvent.invoke()
    }

    override fun onPause() {
        events.onPauseEvent.invoke()
        super.onPause()
    }

    override fun onStop() {
        events.onStopEvent.invoke()
        super.onStop()
    }

    override fun onBackPressed() {
        events.onBackPressedEvent.invoke()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        events.onSaveInstanceStateEvent.invoke(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        events.onActivityResultEvent.invoke(ActivityEvents.ActivityResultData(requestCode, resultCode, data))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        events.onRequestPermissionsResultEvent.invoke(
                ActivityEvents.RequestPermissionsResultData(
                        requestCode,
                        permissions,
                        grantResults
                )
        )
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return events.onPrepareOptionsMenuEvent.invoke(menu) ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return events.onOptionsItemSelectedEvent.invoke(item) ?: false
    }
}