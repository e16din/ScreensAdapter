package com.e16din.screensadapter

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import com.e16din.datamanager.DataManager
import com.e16din.datamanager.getData
import com.e16din.datamanager.putData
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.activities.DefaultActivity
import com.e16din.screensadapter.activities.DialogActivity
import com.e16din.screensadapter.activities.TranslucentActivity
import com.e16din.screensadapter.binders.IScreenBinder
import com.e16din.screensadapter.binders.android.FragmentScreenBinder
import com.e16din.screensadapter.fragments.BaseFragment
import com.e16din.screensadapter.helpers.removeNow
import com.e16din.screensadapter.helpers.replaceNow
import com.e16din.screensadapter.settings.ScreenSettings
import com.e16din.screensmodel.BaseApp
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

//todo: Сделать @BindCase(caseCls, screenCls) биндить автоматически на onBind

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

    private var nextScreenId = Int.MAX_VALUE

    internal var currentActivityRef = WeakReference<BaseActivity>(null)

    private var lastResultRef = WeakReference<Any?>(null)

    // NOTE: Screen -> Screen Object
    val screensMap = hashMapOf<Int, Pair<KClass<*>, Any?>>()

    // NOTE: Screen -> Binder
    //todo: Брать биндеры не по классу а по id-шнику
    //todo: и можно будет удалить parentFragmentScreenId или нельзя, выписать структуру на бумагу и улучшить/сократить по возможности
    internal val mainBindersMap = hashMapOf<KClass<*>, IScreenBinder>()
    // NOTE: MainScreen -> ChildScreen -> Binder
    internal val childBindersMap = hashMapOf<KClass<*>, ConcurrentHashMap<KClass<*>, IScreenBinder>>()
    internal val finishedScreensIds = arrayListOf<Int>() // todo:

    // NOTE: fragmentId -> (FragmentScreen, Binder)
    internal val fragmentBindersMap = hashMapOf<Int, Pair<KClass<*>, IScreenBinder>>()
    // NOTE: parentFragmentScreenId -> childScreenId -> ChildScreen -> Binder
    internal val fragmentChildBindersMap = hashMapOf<Int, ConcurrentHashMap<KClass<*>, IScreenBinder>>()
    internal val finishedFragmentsIds = arrayListOf<Int>()

    @Volatile
    var screenSettingsStack = Stack<ScreenSettings>()

    var onBackPressedListener: (() -> Unit)? = null
    var onOptionsItemSelectedListener: ((item: MenuItem) -> Boolean)? = null
    var onPrepareOptionsMenuListener: ((menu: Menu?) -> Boolean)? = null

    // NOTE: Kowabunga!

    private fun getActivity(settings: ScreenSettings): KClass<*> {
        if (settings.activityCls != Any::class) {
            return settings.activityCls
        }

        return when {
            settings.isDialog -> DialogActivity::class
            settings.isTransluent -> TranslucentActivity::class
            else -> DefaultActivity::class
        }
    }

    private fun startActivity(settings: ScreenSettings, prevSettings: ScreenSettings?, activity: Activity? = null) {
        val starter = activity ?: getCurrentActivity()

        starter?.run {
            getCurrentScreenCls()?.let {
                beforeNextActivityStart(it)
            }

            val intent = Intent(this, getActivity(settings).java)
            if (settings.finishAllPreviousScreens) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            settings.requestCode?.run {
                Log.w(TAG, "settings.requestCode: ${settings.screenCls.simpleName} | requestCode = ${settings.requestCode}")
                startActivityForResult(intent, this)

            } ?: {
                startActivity(intent)
            }.invoke()

            if (settings.finishPreviousScreen) {
                this.finish()
            }

            if (prevSettings?.finishOnNextScreen == true) {
                this.finish()
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

    fun getLastResult() = lastResultRef.get()

    fun backToPreviousScreenOrClose(result: Any? = null) {
        lastResultRef = WeakReference(result)

        val currentScreenCls = getCurrentScreenCls()!!
        Log.i(TAG, "hideCurrentScreen: ${currentScreenCls.simpleName}")

        val binder = mainBindersMap[currentScreenCls]
        Log.d(TAG, "     ${binder!!.javaClass.simpleName}.onPrevScreen()")
        binder.onPrevScreen()

        callForActualChildBinders(currentScreenCls) { childBinder ->
            Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onPrevScreen()")
            childBinder.onPrevScreen()
        }

        popScreenSettings()
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

    abstract fun makeBinder(screenCls: KClass<*>): IScreenBinder

    abstract fun getScreen(screenId: Int,
                           screenCls: KClass<*>?,
                           data: Any? = null,
                           parent: Any? = null,
                           recreate: Boolean): Any

    // NOTE: There are available to use from binders:

    fun getAndroidApp() = androidAppRef.get()
    fun getCurrentActivity() = currentActivityRef.get()

    fun getApp() = appModelRef.get()!!
    fun getServer() = serverModelRef.get()!!

    fun setFirstScreen(settings: ScreenSettings, data: Any? = null) {
        firstScreenSettings = settings
        firstData = data
    }

    //NOTE: add child screens in onBind() or after onFocus() with isScreenFocused == true
    // как это можно улучшить?
    fun addChildBinder(childScreenCls: KClass<*>,
                       data: Any? = null,
                       parent: Any? = null,
                       parentFragmentId: Int = -3,
                       isScreenFocused: Boolean = false,
                       recreateScreen: Boolean = false) {

        val screenId = generateScreenId()

        val screen = getScreen(screenId, childScreenCls, data, parent, recreateScreen)
        screensMap[screenId] = Pair(childScreenCls, screen)

        Log.i(TAG, "addChildBinder(isScreenFocused = $isScreenFocused): ${childScreenCls.simpleName}")
        val binder = makeBinder(childScreenCls)

        val hasFragmentId = parentFragmentId != -3
        if (hasFragmentId) {
            (binder as FragmentScreenBinder<*>).fragmentId = parentFragmentId
            fragmentChildBindersMap[parentFragmentId]!![childScreenCls] = binder

        } else {
            val mainScreenCls = getCurrentScreenCls()!!
            childBindersMap[mainScreenCls]!![childScreenCls] = binder
        }

        binder.initScreen(screen)
        binder.onPrepare()

        if (isScreenFocused) {
            binder.counter += 1
            binder.onBind()
            binder.onShow()
            binder.onFocus()
        }

        saveScreensAdapterState()
    }

    fun removeChildBinder(childScreenCls: KClass<*>, fragmentScreenId: Int = -1) {
        childBindersMap.remove(childScreenCls)
        if (fragmentScreenId >= 0) {
            fragmentChildBindersMap[fragmentScreenId]!!.remove(childScreenCls)
        }

        saveScreensAdapterState()
    }

    fun createFragment(settings: ScreenSettings,
                       data: Any? = null,
                       parent: Any? = null,
                       recreateScreen: Boolean = false): BaseFragment {

        settings.screenId = generateScreenId(settings.screenId)
        fragmentChildBindersMap[settings.screenId] = ConcurrentHashMap()

        val screen = getScreen(settings.screenId, settings.screenCls, data, parent, recreateScreen)
        screensMap[settings.screenId] = Pair(settings.screenCls, screen)

        Log.i(TAG, "fragmentId: ${settings.screenId}")
        val binder = makeBinder(screen.javaClass.kotlin)

        (binder as FragmentScreenBinder<*>).fragmentId = settings.screenId

        fragmentBindersMap[settings.screenId] = Pair(screen.javaClass.kotlin, binder)

        binder.initScreen(screen)

        return BaseFragment.create(settings.screenCls,
                settings.layoutId!!,
                fragmentScreenId = settings.screenId)
    }

    private fun generateScreenId(screenId: Int = ScreenSettings.NO_ID): Int {
        val hasScreenId = screenId != ScreenSettings.NO_ID
        if (hasScreenId) {
            return screenId

        } else {
            nextScreenId -= 1
            return nextScreenId
        }
    }

    fun showFragment(containerId: Int,
                     settings: ScreenSettings,
                     data: Any? = null,
                     parent: Any? = null,
                     fragmentManager: FragmentManager = getCurrentActivity()?.supportFragmentManager!!,
                     recreateScreen: Boolean = false) {
        val fragment = createFragment(settings, data, parent, recreateScreen)
        val tag = "$containerId"

        val previousFragment = (fragmentManager.findFragmentByTag(tag) as BaseFragment?)
        previousFragment?.let {
            finishedFragmentsIds.add(it.fragmentId)
        }

        Log.i(TAG, "showFragmentScreen: ${settings.screenCls.simpleName}")
        fragmentManager.replaceNow(containerId, fragment, tag)

        saveScreensAdapterState()
    }

    fun removeFragment(fragment: BaseFragment,
                       fragmentManager: FragmentManager = getCurrentActivity()?.supportFragmentManager!!) {

        finishedFragmentsIds.add(fragment.fragmentId)
        fragmentManager.removeNow(fragment)

        saveScreensAdapterState()
    }

    fun removeFragment(containerId: Int,
                       fragmentManager: FragmentManager = getCurrentActivity()?.supportFragmentManager!!) {
        val fragment = getCurrentActivity()?.supportFragmentManager
                ?.findFragmentByTag("$containerId")
        fragment?.let {
            removeFragment(it as BaseFragment, fragmentManager)
        }
    }

    fun showNextScreen(settings: ScreenSettings,
                       activity: Activity? = null,
                       data: Any? = null,
                       parent: Any? = null,
                       recreateScreen: Boolean = false) {

        onBackPressedListener = null

        if (settings.finishAllPreviousScreens) {
            if (!screenSettingsStack.isEmpty()) {
                screenSettingsStack.clear()
            }
        }

        val nextScreenCls = settings.screenCls
        childBindersMap[nextScreenCls] = ConcurrentHashMap()

        settings.screenId = generateScreenId(settings.screenId)
        val screen = getScreen(settings.screenId, nextScreenCls, data, parent, recreateScreen)
        screensMap[settings.screenId] = Pair(nextScreenCls, screen)

        Log.i(TAG, "showNextScreen: ${screen.javaClass.simpleName}")
        val binder = makeBinder(screen.javaClass.kotlin)
        mainBindersMap[screen.javaClass.kotlin] = binder

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

    fun resetToFirstScreen(data: Any? = firstData, recreateScreen: Boolean = false) {
        val firstScreenSettings = screenSettingsStack.last()
        firstScreenSettings.finishAllPreviousScreens = true
        showNextScreen(firstScreenSettings, data = data, parent = null, recreateScreen = recreateScreen)
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

    fun hideCurrentScreen(result: Any?) = backToPreviousScreenOrClose(result)
}
