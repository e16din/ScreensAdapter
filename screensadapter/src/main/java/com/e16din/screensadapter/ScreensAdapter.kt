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
import com.e16din.screensadapter.binders.IScreenBinder
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

    // NOTE: Screen -> Binder
    internal val mainBindersMap = hashMapOf<Class<*>, IScreenBinder>()

    // NOTE: fragmentId -> (FragmentScreen, Binder)
    internal val fragmentBindersMap = hashMapOf<Long, Pair<Class<*>, IScreenBinder>>()

    // NOTE: MainScreen -> ChildScreen -> Binder
    internal val childBindersMap = hashMapOf<Class<*>, HashMap<Class<*>, IScreenBinder>>()

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
            getCurrentScreenCls()?.let {
                beforeNextActivityStart(it)
            }

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

    fun backToPreviousScreenOrClose() {
        popScreenSettings()

        val currentScreenCls = getCurrentScreenCls()!!
        Log.i(TAG, "hideCurrentScreen: ${currentScreenCls.simpleName}")

        ActivityCompat.finishAfterTransition(getCurrentActivity()!!)
    }

    private fun saveScreensAdapterState() {
        //todo: Save and restore binders, screens and settings and firstData
        //todo: save all
    }

    private fun restoreScreensAdapterState() {
        //todo: restore all
    }

    private fun pushScreen(settings: ScreenSettings) {
        screenSettingsStack.push(settings)
    }

    private fun popScreenSettings() = screenSettingsStack.pop()

    private fun getCurrentScreenCls() = screenSettingsStack.takeIf { it.size > 0 }
            ?.peek()?.screenCls

    // NOTE: There are used in generated screens adapter:

    protected fun createNotFoundException(name: String) =
            IllegalStateException("Invalid Screen!!! The \"$name\" screen is not found in the generator.")

    protected fun restoreScreen(screenName: String): Any? {
        return screenName.getData()
    }

    abstract fun makeBinder(screenCls: Class<*>): IScreenBinder

    abstract fun makeScreen(mainScreenCls: Class<*>, data: Any?, parent: Any?): Any

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
                       parent: Any? = null,
                       withLifecycleCallsAtStart: Boolean = false) {
        val screen = makeScreen(screenCls, data, parent)

        val childScreenCls = screen.javaClass
        Log.i(TAG, "addChildBinder(withLifecycleCallsAtStart = $withLifecycleCallsAtStart): ${childScreenCls.simpleName}")
        val binder = makeBinder(childScreenCls)

        val mainScreenCls = getCurrentScreenCls()!!
        if (childBindersMap[mainScreenCls] == null) {
            childBindersMap[mainScreenCls] = hashMapOf()
        }
        childBindersMap[mainScreenCls]!![childScreenCls] = binder

        binder.initScreen(screen)

        if (withLifecycleCallsAtStart) {
            binder.onPrepare()
            binder.onBind()
            binder.counter += 1
            binder.onShow(binder.counter)
            binder.onFocus(binder.counter)
        }

        saveScreensAdapterState()
    }

    fun removeChildBinder(screenCls: Class<*>) {
        childBindersMap.remove(screenCls)

        saveScreensAdapterState()
    }

    fun createFragment(settings: ScreenSettings,
                       data: Any? = null,
                       parent: Any? = null,
                       fragmentId: Long = nextFragmentId): BaseFragment {

        val finalFragmentId: Long

        if (fragmentId == nextFragmentId) {
            nextFragmentId -= 1
            finalFragmentId = nextFragmentId

        } else {
            finalFragmentId = fragmentId
        }

        val screen = makeScreen(settings.screenCls, data, parent)

        Log.i(TAG, "fragmentId: $finalFragmentId")
        Log.i(TAG, "showFragmentScreen: ${screen.javaClass.simpleName}")
        val binder = makeBinder(screen.javaClass)

        (binder as FragmentScreenBinder<*>).fragmentId = finalFragmentId

        fragmentBindersMap[finalFragmentId] = Pair(screen.javaClass, binder)

        binder.initScreen(screen)

        return BaseFragment.create(settings.screenCls,
                settings.layoutId!!,
                fragmentId = finalFragmentId)
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
        fragmentBindersMap.forEach { (fragmentId, pair) ->
            if (pair.first == fragment.screenCls) {
                fragmentBindersMap.remove(fragmentId)
                return
            }
        }

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

        removeScreenFromSharedPreferences(settings)
        onBackPressedListener = null

        if (settings.finishAllPreviousScreens) {
            if (!screenSettingsStack.isEmpty()) {
                screenSettingsStack.clear()
            }
        }

        val screen = makeScreen(settings.screenCls, data, parent)

        Log.i(TAG, "showNextScreen: ${screen.javaClass.simpleName}")
        val binder = makeBinder(screen.javaClass)
        mainBindersMap[screen.javaClass] = binder

        binder.initScreen(screen)

        var prevSettings: ScreenSettings? = null
        if (screenSettingsStack.isNotEmpty()) {
            prevSettings = screenSettingsStack.peek()

            if (prevSettings.finishOnNextScreen || settings.finishPreviousScreen) {
                popScreenSettings()

                val prevScreenCls = prevSettings.screenCls
                //todo: ?
            }
        }

        pushScreen(settings)
        startActivity(settings, prevSettings, activity)

        saveScreensAdapterState()
    }

    private fun removeScreenFromSharedPreferences(settings: ScreenSettings) {
        settings.screenCls.name.remove()
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

    fun getCurrentSettings(): ScreenSettings {
        if (screenSettingsStack.isEmpty()) {
            restoreScreensAdapterState()
        }

        return screenSettingsStack.peek()
    }

    fun hideCurrentScreen() = backToPreviousScreenOrClose()
}
