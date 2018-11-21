package com.e16din.screensadapter

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ActivityInfo
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.e16din.datamanager.DataManager
import com.e16din.datamanager.getData
import com.e16din.datamanager.putData
import com.e16din.datamanager.remove
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.activities.DefaultActivity
import com.e16din.screensadapter.activities.LandscapeActivity
import com.e16din.screensadapter.activities.PortraitActivity
import com.e16din.screensadapter.binders.android.BaseAndroidScreenBinder
import com.e16din.screensadapter.settings.ScreenSettings
import com.e16din.screensmodel.BaseApp
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set

abstract class ScreensAdapter<out APP : BaseApp, out SERVER>(
        androidApp: Application,
        appModel: APP,
        serverModel: SERVER,
        private val delayForSplashMs: Long = 1500) {

    companion object {
        private const val TAG = "screensAdapter.debug"
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
    private val bindersByScreenClsMap = hashMapOf<Class<*>, Collection<BaseAndroidScreenBinder>>()
    private val hiddenBinders: ArrayList<BaseAndroidScreenBinder> = arrayListOf()

    private var screenSettingsStack = Stack<ScreenSettings>()

    private lateinit var firstScreenSettings: ScreenSettings

    private var showScreenInProgress = false

    var onBackPressed: (() -> Unit)? = null
    var onOptionsItemSelected: ((item: MenuItem) -> Boolean)? = null
    var onPrepareOptionsMenu: ((menu: Menu?) -> Boolean)? = null

    private fun getCurrentBinders(): Collection<BaseAndroidScreenBinder> {
        if (screenSettingsStack.isEmpty()) {
            return emptyList()
        }

        val screens = getCurrentScreens()
        val allBinders = ArrayList<BaseAndroidScreenBinder>()
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
            if (settings.finishAllPreviousScreens) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    //NOTE: it is used in generated screens adapter
    protected fun createNotFoundException(name: String) =
            IllegalStateException("Invalid Screen!!! The \"$name\" screen is not found in the generator.")

    abstract fun generateBinders(screenCls: Class<*>): Collection<BaseAndroidScreenBinder>

    abstract fun generateScreens(mainScreenCls: Class<*>, data: Any?): Collection<Any>

    fun restoreScreen(screenName: String): Any? {
        return screenName.getData()
    }

    // BaseActivity callbacks

    fun onStarterActivityCreated(activity: Activity) {
        Log.i(TAG, "show starter!")
        GlobalScope.launch {
            delay(delayForSplashMs)
            launch(Main) {
                showNextScreen(firstScreenSettings, activity)
                ActivityCompat.finishAfterTransition(activity)
            }
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

    fun onActivityCreated(activity: BaseActivity) {
        currentActivityRef = WeakReference(activity)
        val binders = getCurrentBinders()
        binders.forEach { binder ->
            binder.onBind()
        }
    }

    fun onBackPressed() {
        Log.w(TAG, "screenSettingsStack: $screenSettingsStack")

        if (onBackPressed != null) {
            onBackPressed?.invoke()
            return
        }

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

    fun showNextScreen(settings: ScreenSettings, activity: Activity? = null, data: Any? = null) {
        settings.screenCls.name.remove()
        onBackPressed = null

        val screens = generateScreens(settings.screenCls, data)
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

        pushScreen(settings)
        startActivity(settings, activity)
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
        val launchNumber = DATA.LAUNCH_NUMBER.getData() ?: 0
        getApp().onStart(launchNumber)

        DATA.LAUNCH_NUMBER.putData(launchNumber + 1)
    }

    fun resetToFirstScreen() {
        val firstScreenSettings = screenSettingsStack.last()
        firstScreenSettings.finishAllPreviousScreens = true
        showNextScreen(firstScreenSettings)
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return onOptionsItemSelected?.invoke(item) ?: false
    }

    fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return onPrepareOptionsMenu?.invoke(menu) ?: true
    }

    fun getCurrentSettings() = screenSettingsStack.peek()
}
