package com.e16din.screensadapter

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityCompat
import com.e16din.datamanager.DataManager
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.activities.DialogActivity
import com.e16din.screensadapter.activities.TranslucentActivity
import java.lang.ref.WeakReference
import kotlin.reflect.KClass


open class ScreensAdapter(androidApp: Application) {

    companion object {
        private const val TAG = "SA.Core"

        lateinit var get: ScreensAdapter
    }

    object DATA {
        const val ITEMS = "ITEMS"
    }

    var restoreScreensOnStart = true

    var nextScreenId = Int.MAX_VALUE

    var currentActivityRef = WeakReference<BaseActivity>(null)

    var items: MutableList<ScreenSettings> = mutableListOf()

    init {
        get = this

        DataManager.initDefaultDataBox(androidApp)

        if (restoreScreensOnStart) {
            restoreState()
        }
    }

    var onBackPressedListener: (() -> Unit)? = null

    var baseActivitySymbiont: Any? = null

    // NOTE: Kowabunga!

    fun getActivityCls(settings: ScreenSettings): KClass<*> {
        when {
            settings.isDialog -> return DialogActivity::class
            settings.isTranslucent -> return TranslucentActivity::class
        }

        return settings.activityCls
    }

    private fun startActivity(settings: ScreenSettings) {
        val starter = getCurrentActivity()

        starter?.run {
            beforeNextActivityStart(settings)

            val intent = Intent(this, getActivityCls(settings).java)
            if (settings.finishAllPreviousScreens) {
                items.clear()
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            settings.requestCode?.run {
                Log.w(TAG, "items.requestCode: ${settings.activityCls.simpleName} | requestCode = ${settings.requestCode}")
                startActivityForResult(intent, this)

            } ?: {
                startActivity(intent)
            }.invoke()

            if (settings.finishPreviousScreen) {
                items.removeAt(items.lastIndex)
                ActivityCompat.finishAfterTransition(this)
            }

            val hasPrevSettings = items.isNotEmpty()
            if (hasPrevSettings) {
                val prevSettings = items[items.lastIndex]
                if (prevSettings.finishOnNextScreen) {
                    items.removeAt(items.lastIndex)
                    ActivityCompat.finishAfterTransition(this)
                }
            }
        }
    }

    protected open fun beforeNextActivityStart(settings: ScreenSettings) {
        // NOTE: for override
    }

    open fun backToPreviousScreenOrClose(withAnimation: Boolean, resultCode: Int?) {
        val activitySimpleName = items.lastOrNull()?.activityCls?.simpleName
        Log.i(TAG, "hideCurrentScreen: $activitySimpleName")

        if (items.size > 1) { // NOTE (> 1): Если загружать с ленты приложений то приложение не перезапускается, а восстанавливается activity
            items.removeAt(items.lastIndex)
        }

        val currentActivity = getCurrentActivity()!!
        resultCode?.let {
            currentActivity.setResult(resultCode)
        }
        if (withAnimation) {
            ActivityCompat.finishAfterTransition(currentActivity)

        } else {
            currentActivity.finish()
            currentActivity.overridePendingTransition(0, 0)
        }
    }

    fun getCurrentActivity() = currentActivityRef.get()

    fun generateScreenId(screenId: Int = ScreenSettings.NO_ID): Int {
        val hasScreenId = screenId != ScreenSettings.NO_ID
        if (hasScreenId) {
            return screenId

        } else {
            nextScreenId -= 1
            return nextScreenId
        }
    }

    fun showScreenAt(position: Int) {
        backToScreen(position)
    }

    fun showNextScreen(settings: ScreenSettings) {
        prepareScreen(settings)
        startActivity(settings)
        items.add(settings)
    }

    fun backToScreen(position: Int) {
        if (items.size > position) {
            val starter = getCurrentActivity()

            starter?.run {
                val settings = items[position]
                beforeNextActivityStart(settings)

                val intent = Intent(this, getActivityCls(settings).java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }

        } else {
            throw IllegalStateException("Incorrect settings position: $position")
        }
    }

    fun getSettingsPosition(screenId: Int) =
            items.indexOfLast { it.screenId == screenId }

    open fun prepareScreen(settings: ScreenSettings) {
        if (settings.screenId == ScreenSettings.NO_ID) {
            settings.screenId = generateScreenId(settings.screenId)
        }
        Log.i(TAG, "prepareScreen: ${settings.activityCls.simpleName}")
    }

    fun resetToFirstScreen() {
        val firstScreenSettings = items.first().apply {
            finishAllPreviousScreens = true
        }
        showNextScreen(firstScreenSettings)
    }

    fun start() {
        Log.i(TAG, "start(): see FirstActivity")
    }

    fun hideCurrentScreen(withAnimation: Boolean, resultCode: Int?) =
            backToPreviousScreenOrClose(withAnimation, resultCode)

    fun saveState() {
//        val builder = GsonBuilder()
//        val type = object : TypeToken<KClass<*>>() {}.type
//        builder.registerTypeAdapter(type, KClassAdapter())
//        val gson = builder.create()
//
//        val json = gson.toJson(items)
//        DATA.ITEMS.putData(json)
    }

    fun restoreState() {
//        val json = DATA.ITEMS.getData() ?: ""
//
//        if (json.isNullOrBlank()) {
//            return
//        }
//
//        val builder = GsonBuilder()
//        val type = object : TypeToken<KClass<*>>() {}.type
//        builder.registerTypeAdapter(type, KClassAdapter())
//        val gson = builder.create()
//
//        val screenSettingsType = object : TypeToken<MutableList<ScreenSettings>>() {}.type
//        items = gson.fromJson(json, screenSettingsType)
    }

    // NOTE: for override
    open fun createActivitySymbiont(): Any? = BaseActivitySymbiont()
}
