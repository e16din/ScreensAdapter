package com.e16din.screensadapter

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.e16din.datamanager.DataManager
import com.e16din.datamanager.get
import com.e16din.datamanager.put
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.activities.DefaultActivity
import com.e16din.screensadapter.activities.LandscapeActivity
import com.e16din.screensadapter.activities.PortraitActivity
import com.e16din.screensadapter.binders.BaseScreenBinder
import com.e16din.screensadapter.settings.ScreenSettings
import com.e16din.screensmodel.AppModel
import com.e16din.screensmodel.ServerModel
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set

abstract class ScreensAdapter<out APP : AppModel, out SERVER : ServerModel>(
        androidApp: Application,
        appModel: APP,
        serverModel: SERVER,
        private val delayForSplashMs: Int = 1500) {

    companion object {
        private const val TAG = "adapter.debug"
    }

    object DATA {
        const val LAUNCH_NUMBER = "LAUNCH_NUMBER"
    }

    init {
        DataManager.initDefaultDataBox(androidApp)
    }

    private val androidAppRef = WeakReference<Application>(androidApp)
    private var currentActivityRef = WeakReference<BaseActivity>(null)

    private var appModelRef = WeakReference<APP>(appModel)
    private var serverModelRef = WeakReference<SERVER>(serverModel)

    // Note: MainScreen -> Screens
    private val screensByMainScreenClsMap = hashMapOf<Class<*>, Collection<Any>>()
    // Note: Screen -> Binders
    private val bindersByScreenClsMap = hashMapOf<Class<*>, Collection<BaseScreenBinder>>()
    private val hiddenBinders: ArrayList<BaseScreenBinder> = arrayListOf()

    private val screenSettingsStack = Stack<ScreenSettings>()

    private lateinit var firstScreenSettings: ScreenSettings

    private var showScreenInProgress = false

    private fun getCurrentBinders(): Collection<BaseScreenBinder> {
        if (screenSettingsStack.isEmpty()) {
            return emptyList()
        }

        val screens = getCurrentScreens()
        val allBinders = ArrayList<BaseScreenBinder>()
        screens.forEach { screen ->
            val bindersForScreen = bindersByScreenClsMap[screen.javaClass]
                    ?: throw NullPointerException("Array of binders must not be null!")

            bindersForScreen.forEach { newBinder ->
                var isUnique = true
                allBinders.forEach { binder ->
                    if (binder.javaClass == newBinder.javaClass) {
                        isUnique = false
                    }
                }

                if (isUnique) {
                    allBinders.add(newBinder)
                }
            }
        }

        return allBinders
    }

    private fun getActivity(settings: ScreenSettings): Class<*> {
        if (settings.activityCls != Any::class.java) {
            return settings.activityCls
        }

        return when (settings.orientation) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> LandscapeActivity::class.java
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> PortraitActivity::class.java
            else -> DefaultActivity::class.java
        }
    }

    private fun startActivity(settings: ScreenSettings, activity: Activity? = null) {
        val starter = activity
                ?: getCurrentActivity()

        starter?.run {
            val intent = Intent(this, getActivity(settings))
            intent.putExtra(BaseActivity.KEY_SCREEN_SETTINGS, settings)
            if (settings.finishAllPreviousScreens) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    protected fun createNotFoundException(name: String) =
            IllegalStateException("Invalid Screen!!! The \"$name\" screen is not found in the generator.")

    abstract fun generateBinders(screenCls: Class<*>): Collection<BaseScreenBinder>

    abstract fun generateScreens(mainScreenCls: Class<*>): Collection<Any>

    // BaseActivity callbacks

    fun onStarterActivityCreated(activity: Activity) {
        Log.i(TAG, "show starter!")
        launch {
            delay(delayForSplashMs)
            launch(UI) {
                showNextScreen(firstScreenSettings, activity)
                ActivityCompat.finishAfterTransition(activity)
            }
        }
    }

    fun onActivityResume() {
        val binders = getCurrentBinders()
        binders.forEach { binder ->
            binder.onFocus()
        }
    }

    fun onActivityPause() {
        val binders = hiddenBinders.takeIf { hiddenBinders.isNotEmpty() }
                ?: getCurrentBinders()
        binders.forEach { binder ->
            binder.onLostFocus()
        }
    }

    fun onActivityStop(activity: BaseActivity) {
        val binders = hiddenBinders.takeIf { hiddenBinders.isNotEmpty() }
                ?: getCurrentBinders()
        binders.forEach { binder ->
            binder.onHide()
        }
        hiddenBinders.clear()

        val isSameActivity = getCurrentActivity()?.equals(activity) == true
        if (isSameActivity) {
            showScreenInProgress = false
        }
        if (!showScreenInProgress) {
            getApp().onHideAllScreens(screenSettingsStack.size)
        }
    }

    fun onActivityStart(activity: BaseActivity) {
        showScreenInProgress = true
        currentActivityRef = WeakReference(activity)

        val binders = getCurrentBinders()
        binders.forEach { binder ->
            binder.onShow()
        }
    }

    fun onActivityCreated(activity: BaseActivity) {
        currentActivityRef = WeakReference(activity)
        val binders = getCurrentBinders()
        binders.forEach { binder ->
            binder.onBind()
        }
    }

    fun onBackPressed() {
        Log.w(TAG, "screenSettingsStack: $screenSettingsStack")
        if (screenSettingsStack.isNotEmpty()) {
            backToPreviousScreenOrClose()
        }
    }

    private fun backToPreviousScreenOrClose() {
        val currentScreenSettings = popScreen()
        Log.i(TAG, "hideCurrentScreen: ${currentScreenSettings.screenCls.simpleName}")

        val binders = bindersByScreenClsMap[currentScreenSettings.screenCls]!!
        hiddenBinders.clear()
        hiddenBinders.addAll(binders)

        bindersByScreenClsMap[currentScreenSettings.screenCls] = emptyList()

        getCurrentActivity()?.run {
            ActivityCompat.finishAfterTransition(this)
        }
    }

    // There are available to use from binders:

    fun getAndroidApp() = androidAppRef.get()
    fun getCurrentActivity() = currentActivityRef.get()

    fun getApp() = appModelRef.get()!!
    fun getServer() = serverModelRef.get()!!

    fun setFirstScreen(settings: ScreenSettings) {
        firstScreenSettings = settings
    }

    fun showNextScreen(settings: ScreenSettings, activity: Activity? = null) {
        val screens = generateScreens(settings.screenCls)
        screensByMainScreenClsMap[settings.screenCls] = screens

        screens.forEach { screen ->
            Log.i(TAG, "showNextScreen: ${screen.javaClass.simpleName}")
            val binders = generateBinders(screen.javaClass)
            bindersByScreenClsMap[screen.javaClass] = binders.filter { it.isEnabled() }
            binders.forEach { it.setScreens(screens) }
        }

        if (screenSettingsStack.isNotEmpty()) {
            val prevSettings = screenSettingsStack.peek()

            val prevBinders = bindersByScreenClsMap[prevSettings.screenCls]!!
            hiddenBinders.clear()
            hiddenBinders.addAll(prevBinders)

            if (prevSettings.finishOnNextScreen) {
                popScreen()
                bindersByScreenClsMap[prevSettings.screenCls] = emptyList()
            }
        }

        if (settings.finishAllPreviousScreens) {
            screenSettingsStack.clear()
            bindersByScreenClsMap.clear()
        }

        startActivity(settings, activity)
        pushScreen(settings)
    }

    private fun pushScreen(settings: ScreenSettings) {
        screenSettingsStack.push(settings)
    }

    private fun popScreen() = screenSettingsStack.pop()

    fun hideCurrentScreen() = backToPreviousScreenOrClose()

    fun getCurrentScreen(): Any {
        val settings = screenSettingsStack.takeIf { it.size > 0 }
                ?.peek()
        return screensByMainScreenClsMap[settings?.screenCls]?.first()
                ?: throw NullPointerException("Screen must be not null!")
    }

    fun getCurrentScreens(): Collection<Any> {
        val settings = screenSettingsStack.takeIf { it.size > 0 }
                ?.peek()
        return screensByMainScreenClsMap[settings?.screenCls]
                ?: throw NullPointerException("Init supportScreens for settings before use")
    }

    fun start() {
        val launchNumber = DATA.LAUNCH_NUMBER.get() ?: 0
        getApp().onStart(launchNumber)

        DATA.LAUNCH_NUMBER.put(launchNumber + 1)
    }

    fun resetToFirstScreen() {
        val firstScreenSettings = screenSettingsStack.last()
        firstScreenSettings.finishAllPreviousScreens = true
        showNextScreen(firstScreenSettings)
    }
}
