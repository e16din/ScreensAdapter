package com.e16din.screensadapter

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import com.e16din.datamanager.DataManager
import com.e16din.datamanager.getData
import com.e16din.datamanager.putData
import com.e16din.datamanager.remove
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.activities.DefaultActivity
import com.e16din.screensadapter.activities.DialogActivity
import com.e16din.screensadapter.binders.android.BaseAndroidScreenBinder
import com.e16din.screensadapter.binders.android.FragmentScreenBinder
import com.e16din.screensadapter.fragments.BaseFragment
import com.e16din.screensadapter.settings.ScreenSettings
import com.e16din.screensmodel.BaseApp
import java.lang.ref.WeakReference
import java.util.*

abstract class ScreensAdapter<out APP : BaseApp, out SERVER>(
        androidApp: Application,
        appModel: APP,
        serverModel: SERVER,
        private val delayForSplashMs: Long = 1500) {

    companion object {
        private const val TAG = "SA.Core"
    }

    object DATA {
        const val LAUNCH_NUMBER = "LAUNCH_NUMBER"
    }

    init {
        DataManager.initDefaultDataBox(androidApp)
    }

    private val androidAppRef = WeakReference<Application>(androidApp)

    private var appModelRef = WeakReference<APP>(appModel)
    private var serverModelRef = WeakReference<SERVER>(serverModel)

    private lateinit var firstScreenSettings: ScreenSettings
    private var firstData: Any? = null

    private var nextFragmentId = Long.MAX_VALUE

    internal var currentActivityRef = WeakReference<BaseActivity>(null)

    // Note: MainScreen -> Screens
    @Deprecated("See addChildBinder()") //todo: remove it and add "currentScreenCls" variable
    internal val supportScreensByMainScreenClsMap = hashMapOf<Class<*>, Collection<Any>>()
    // Note: Screen -> Binders
    internal val bindersByScreenClsMap = hashMapOf<Class<*>, Collection<BaseAndroidScreenBinder>>()
    // Note: Screen -> fragmentId -> Binders
    internal val fragmentBindersByScreenClsMap = hashMapOf<Class<*>, MutableMap<Long, List<BaseAndroidScreenBinder>>>()
    // Note: MainScreen -> ChildScreen -> Binders
    internal val childBindersByMainScreenClsMap = hashMapOf<Class<*>, MutableMap<Class<*>, List<BaseAndroidScreenBinder>>>()

    internal val hiddenBinders: ArrayList<BaseAndroidScreenBinder> = arrayListOf()

    var screenSettingsStack = Stack<ScreenSettings>()

    var onBackPressedListener: (() -> Unit)? = null
    var onOptionsItemSelectedListener: ((item: MenuItem) -> Boolean)? = null
    var onPrepareOptionsMenuListener: ((menu: Menu?) -> Boolean)? = null

    // NOTE: Kowabunga!

    private fun getActivity(settings: ScreenSettings): Class<*> {
        if (settings.activityCls != Any::class.java) {
            return settings.activityCls
        }

        return if (settings.isDialog)
            DialogActivity::class.java
        else
            DefaultActivity::class.java
    }

    private fun startActivity(settings: ScreenSettings, prevSettings: ScreenSettings?, activity: Activity? = null) {
        val starter = activity ?: getCurrentActivity()

        starter?.run {
            val intent = Intent(this, getActivity(settings))
            if (settings.finishAllPreviousScreens) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            settings.requestCode?.run {
                Log.w(TAG, "settings.requestCode: ${settings.screenCls.simpleName} | requestCode = ${settings.requestCode}")
                startActivityForResult(intent, this)
            } ?: startActivity(intent)

            if (settings.finishPreviousScreen) {
                ActivityCompat.finishAfterTransition(this)
            }

            if (prevSettings?.finishOnNextScreen == true) {
                ActivityCompat.finishAfterTransition(this)
            }
        }
    }

    internal fun onStarterActivityCreated(activity: Activity) {
        Handler().postDelayed({
            Log.i(TAG, "show starter!")
            showNextScreen(firstScreenSettings, activity, data = firstData, parent = null)
            ActivityCompat.finishAfterTransition(activity)
        }, delayForSplashMs)
    }

    internal fun backToPreviousScreenOrClose() {
        val currentScreenSettings = popScreen()
        Log.i(TAG, "hideCurrentScreen: ${currentScreenSettings.screenCls.simpleName}")

        setHiddenBinders(currentScreenSettings.screenCls)

        bindersByScreenClsMap[currentScreenSettings.screenCls] = emptyList()

        val currentActivity = getCurrentActivity()
        currentActivity?.run {
            ActivityCompat.finishAfterTransition(this)
        }
    }

    private fun saveScreensAdapterState() {
        //todo: Save and restore binders, screens and settings and firstData
        //todo: save all
    }

    private fun restoreScreensAdapterState() {
        //todo: restore all
    }

    private fun setHiddenBinders(screenCls: Class<*>) {
        hiddenBinders.clear()

        val binders = bindersByScreenClsMap[screenCls]!!
        hiddenBinders.addAll(binders)

        val childBindersByChildScreenClsMap = childBindersByMainScreenClsMap[getCurrentScreenCls()]
        childBindersByChildScreenClsMap?.forEach { entry ->
            val childBinders = entry.value
            hiddenBinders.addAll(childBinders)
        }
    }

    private fun pushScreen(settings: ScreenSettings) {
        screenSettingsStack.push(settings)
    }

    private fun popScreen() = screenSettingsStack.pop()

    private fun getCurrentScreenCls() = screenSettingsStack.takeIf { it.size > 0 }
            ?.peek()?.screenCls

    // NOTE: There are used in generated screens adapter

    protected fun createNotFoundException(name: String) =
            IllegalStateException("Invalid Screen!!! The \"$name\" screen is not found in the generator.")

    protected  fun restoreScreen(screenName: String): Any? {
        return screenName.getData()
    }

    abstract fun generateBinders(screenCls: Class<*>): Collection<BaseAndroidScreenBinder>

    abstract fun generateScreens(mainScreenCls: Class<*>, data: Any?, parent: Any?): Collection<Any>

    // NOTE: There are available to use from binders:

    fun getAndroidApp() = androidAppRef.get()
    fun getCurrentActivity() = currentActivityRef.get()

    fun getApp() = appModelRef.get()!!
    fun getServer() = serverModelRef.get()!!

    fun setFirstScreen(settings: ScreenSettings, data: Any? = null) {
        firstScreenSettings = settings
        firstData = data
    }

    fun addChildBinder(screenCls: Class<*>,
                       data: Any? = null,
                       parent: Any? = null) {
        val screens = generateScreens(screenCls, data, parent)

        screens.forEach { screen ->
            Log.i(TAG, "addChildBinder: ${screen.javaClass.simpleName}")
            val binders = generateBinders(screen.javaClass)
            val enabledBinders = binders.filter { it.isEnabled() }

            childBindersByMainScreenClsMap[getCurrentScreenCls()!!] = hashMapOf<Class<*>, List<BaseAndroidScreenBinder>>(Pair(screen.javaClass, enabledBinders))
            binders.forEach {
                it.setScreens(screens)
            }

            binders.forEach {
                it.onPrepare()
                it.onBind()
                it.onShow()
                it.onFocus()
            }
        }
    }

    fun removeChildBinder(screenCls: Class<*>) {
        childBindersByMainScreenClsMap[getCurrentScreenCls()]?.remove(screenCls)
    }

    fun createFragment(settings: ScreenSettings,
                       data: Any? = null,
                       parent: Any? = null,
                       fragmentId: Long = nextFragmentId): BaseFragment {
        nextFragmentId -= 1

        val screens = generateScreens(settings.screenCls, data, parent)

        screens.forEach { screen ->
            Log.i(TAG, "fragmentId: $fragmentId")
            Log.i(TAG, "showFragmentScreen: ${screen.javaClass.simpleName}")
            val binders = generateBinders(screen.javaClass)
            val enabledBinders = binders.filter { it.isEnabled() }
            val fragmentBindersById =
                    fragmentBindersByScreenClsMap[screen.javaClass] ?: hashMapOf()
            enabledBinders.forEach { (it as FragmentScreenBinder<*>).fragmentId = fragmentId }
            fragmentBindersById[fragmentId] = enabledBinders
            fragmentBindersByScreenClsMap[screen.javaClass] = fragmentBindersById
            binders.forEach {
                it.setScreens(screens)
            }
        }

        return BaseFragment.create(settings.screenCls,
                settings.layoutId!!,
                fragmentId = fragmentId)
    }

    fun showFragment(containerId: Int,
                     settings: ScreenSettings,
                     data: Any? = null,
                     parent: Any? = null,
                     tag: String = settings.screenCls.name) {

        val fragment = createFragment(settings, data, parent)

        getCurrentActivity()?.supportFragmentManager
                ?.replaceNow(containerId, fragment, tag)

        saveScreensAdapterState()
    }

    fun removeFragment(fragment: BaseFragment) {
        fragmentBindersByScreenClsMap.remove(fragment.screenCls)
        getCurrentActivity()?.supportFragmentManager
                ?.removeNow(fragment)

        saveScreensAdapterState()
    }

    fun removeFragment(fragmentTag: String) {
        val fragment = getCurrentActivity()?.supportFragmentManager
                ?.findFragmentByTag(fragmentTag)

        removeFragment(fragment as BaseFragment)
    }

    fun showNextScreen(settings: ScreenSettings,
                       activity: Activity? = null,
                       data: Any? = null,
                       parent: Any? = null) {

//        fragmentBindersByScreenClsMap.clear() //todo:

        settings.screenCls.name.remove() // remove from shared preferences
        onBackPressedListener = null

        if (settings.finishAllPreviousScreens) {
            setHiddenBinders(getCurrentSettings().screenCls)
            screenSettingsStack.clear()
        }

        val screens = generateScreens(settings.screenCls, data, parent)
//        supportScreensByMainScreenClsMap.clear() //todo:
        supportScreensByMainScreenClsMap[settings.screenCls] = screens

//        bindersByScreenClsMap.clear() //todo:
        screens.forEach { screen ->
            Log.i(TAG, "showNextScreen: ${screen.javaClass.simpleName}")
            val binders = generateBinders(screen.javaClass)
            bindersByScreenClsMap[screen.javaClass] = binders.filter { it.isEnabled() }
            binders.forEach { it.setScreens(screens) }
        }

        var prevSettings: ScreenSettings? = null
        if (screenSettingsStack.isNotEmpty()) {
            prevSettings = screenSettingsStack.peek()

            setHiddenBinders(prevSettings.screenCls)

            if (prevSettings.finishOnNextScreen || settings.finishPreviousScreen) {
                popScreen()
                bindersByScreenClsMap[prevSettings.screenCls] = emptyList()
            }
        }

        pushScreen(settings)
        startActivity(settings, prevSettings, activity)

        saveScreensAdapterState()
    }

    fun resetToFirstScreen(data: Any? = firstData) {
        val firstScreenSettings = screenSettingsStack.last()
        firstScreenSettings.finishAllPreviousScreens = true
        showNextScreen(firstScreenSettings, data = data, parent = null)
    }

    fun start() {
        val launchNumber = DATA.LAUNCH_NUMBER.getData() ?: 0
        getApp().onStart(launchNumber)

        DATA.LAUNCH_NUMBER.putData(launchNumber + 1)
    }

    fun getCurrentScreen(): Any {
        val screenCls = getCurrentScreenCls()
        return supportScreensByMainScreenClsMap[screenCls]?.first()
                ?: throw NullPointerException("Screen must be not null!")
    }

    fun getCurrentSettings(): ScreenSettings {
        if (screenSettingsStack.isEmpty()) {
            restoreScreensAdapterState()
        }

        return screenSettingsStack.peek()
    }

    fun hideCurrentScreen() = backToPreviousScreenOrClose()
}
