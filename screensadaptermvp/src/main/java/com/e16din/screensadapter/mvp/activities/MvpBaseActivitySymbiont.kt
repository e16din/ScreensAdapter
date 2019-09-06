package com.e16din.screensadapter.mvp.activities

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
        val screensAdapter = getScreensAdapter()
        val settings = getMvpScreenSettings()

        activity.onCreateBeforeSuperCallEvent.addListener {
            screensAdapter.onActivityCreateBeforeSuperCalled(activity, settings.presenterCls)
        }

        activity.onCreateEvent.addListener {
            screensAdapter.onActivityCreated(activity, settings.presenterCls)
        }

        activity.onSaveInstanceStateEvent.addListener {
            screensAdapter.saveState()
        }

        activity.onStartEvent.addListener {
            screensAdapter.onActivityStart(activity, settings.presenterCls)
        }

        activity.onResumeEvent.addListener {
            screensAdapter.onActivityResume(activity, settings.presenterCls)
        }

        activity.onPauseEvent.addListener {
            screensAdapter.onActivityPause(settings.presenterCls)
        }

        activity.onStopEvent.addListener {
            screensAdapter.onActivityStopAfterTransition(activity, settings.presenterCls)
        }

        activity.onActivityResultEvent.addListener {
            screensAdapter.onActivityResult(activity, it!!.requestCode, it.resultCode, it.data, settings.presenterCls)
        }

        activity.onRequestPermissionsResultEvent.addListener {
            screensAdapter.onRequestPermissionsResult(it!!.requestCode, it.permissions, it.grantResults, settings.presenterCls)
        }

        activity.onOptionsItemSelectedEvent.addListener { item ->
            screensAdapter.onOptionsItemSelected(item!!)
        }

        activity.onPrepareOptionsMenuEvent.addListener { menu ->
            screensAdapter.onPrepareOptionsMenu(menu)
        }

        activity.onBackPressedEvent.setListener {
            screensAdapter.onBackPressedListener?.invoke()
                    ?: screensAdapter.onBackPressed(settings.presenterCls)
        }
    }

    private fun getScreensAdapter() = ScreensAdapter.get as MvpScreensAdapter<*, *>
    private fun getMvpScreenSettings() = ScreensAdapter.get.items.last() as MvpScreenSettings
}