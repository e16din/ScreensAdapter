package com.e16din.screensadapter.activities.richactivity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.e16din.screensadapter.helpers.EventHolder

class ActivityEvents {
    var onCreateBeforeSuperCallEvent = EventHolder<Bundle, Nothing>(name = "ActivityEvents.onCreateBeforeSuperCallEvent")
    var onCreateEvent = EventHolder<Bundle, Nothing>(name = "ActivityEvents.onCreateEvent")
    var onPostCreateEvent = EventHolder<Bundle, Nothing>(name = "ActivityEvents.onPostCreateEvent")
    var onStartEvent = EventHolder<Nothing, Nothing>(name = "ActivityEvents.onStartEvent")
    var onResumeEvent = EventHolder<Nothing, Nothing>(name = "ActivityEvents.onResumeEvent")
    var onStopEvent = EventHolder<Nothing, Nothing>(name = "ActivityEvents.onStopEvent")
    var onPauseEvent = EventHolder<Nothing, Nothing>(name = "ActivityEvents.onPauseEvent")
    var onBackPressedEvent = EventHolder<Nothing, Boolean>(name = "ActivityEvents.onBackPressedEvent")

    var onSaveInstanceStateEvent = EventHolder<Bundle, Nothing>(name = "ActivityEvents.onSaveInstanceStateEvent")

    data class ActivityResultData(val requestCode: Int, val resultCode: Int, val data: Intent?)

    var onActivityResultEvent = EventHolder<ActivityResultData, Nothing>(name = "ActivityEvents.onActivityResultEvent")

    data class RequestPermissionsResultData(
        val requestCode: Int,
        val permissions: Array<out String>,
        val grantResults: IntArray
    )

    var onRequestPermissionsResultEvent =
            EventHolder<RequestPermissionsResultData, Nothing>(name = "ActivityEvents.onRequestPermissionsResultEvent")

    // NOTE: Use "EventHolder.result = true" to return true
    var onPrepareOptionsMenuEvent = EventHolder<Menu, Boolean>(name = "ActivityEvents.onPrepareOptionsMenuEvent")
    var onOptionsItemSelectedEvent = EventHolder<MenuItem, Boolean>(name = "ActivityEvents.onOptionsItemSelectedEvent")
}