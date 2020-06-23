package com.e16din.screensadapter.mvp.activities

import android.util.Log
import com.e16din.screensadapter.BaseActivitySymbiont
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.helpers.addListener
import com.e16din.screensadapter.helpers.setListener
import com.e16din.screensadapter.mvp.*


class MvpBaseActivitySymbiont : BaseActivitySymbiont() {

    init {
        BaseActivity.symbiontListener = { activity ->
            initListeners(activity)
        }
    }

    override fun initListeners(activity: BaseActivity) {
        activity.resetAll()
        activity.initListeners()

        val screensAdapter = getScreensAdapter()
        val settings = getMvpScreenSettings()

        activity.events.onCreateBeforeSuperCallEvent.addListener {
            screensAdapter.onActivityCreateBeforeSuperCalled(activity, settings.presenterCls)
        }

        activity.events.onCreateEvent.addListener {
            screensAdapter.onActivityCreated(activity, settings.presenterCls)
        }

        activity.events.onSaveInstanceStateEvent.addListener {
            screensAdapter.saveState()
        }

        activity.events.onStartEvent.addListener {
            screensAdapter.onActivityStart(activity, settings.presenterCls)
        }

        activity.events.onResumeEvent.addListener {
            screensAdapter.onActivityResume(activity, settings.presenterCls)
        }

        activity.events.onPauseEvent.addListener {
            screensAdapter.onActivityPause(settings.presenterCls)
        }

        activity.events.onStopEvent.addListener {
            screensAdapter.onActivityStopAfterTransition(activity, settings.presenterCls)
        }

        activity.events.onActivityResultEvent.addListener {
            Log.i("EH.debug", "onActivityResultEvent: ${settings.presenterCls.simpleName}")
            screensAdapter.onActivityResult(activity, it!!.requestCode, it.resultCode, it.data, settings.presenterCls)
        }

        activity.events.onRequestPermissionsResultEvent.addListener {
            screensAdapter.onRequestPermissionsResult(
                    it!!.requestCode,
                    it.permissions,
                    it.grantResults,
                    settings.presenterCls
            )
        }

        activity.events.onOptionsItemSelectedEvent.addListener { item ->
            screensAdapter.onOptionsItemSelected(item!!)
        }

        activity.events.onPrepareOptionsMenuEvent.addListener { menu ->
            screensAdapter.onPrepareOptionsMenu(menu)
        }

        val onBackPressedEvent = activity.events.onBackPressedEvent
        onBackPressedEvent.setListener {
            onBackPressedEvent.result = true
            screensAdapter.onBackPressedListener?.invoke()
                    ?: screensAdapter.onBackPressed(settings.presenterCls)
        }
    }

    private fun getScreensAdapter() = ScreensAdapter.get as MvpScreensAdapter<*, *>
    private fun getMvpScreenSettings() = ScreensAdapter.get.items.last() as MvpScreenSettings
}