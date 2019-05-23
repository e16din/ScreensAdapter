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
import kotlin.collections.ArrayList

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
    // Note: Screen -> fragmentId -> Binders
    private val fragmentBindersByScreenClsMap = hashMapOf<Class<*>, HashMap<Long, List<BaseAndroidScreenBinder>>>()
    // Note: MainScreen -> ChildScreen -> Binders
    private val childBindersByMainScreenClsMap = hashMapOf<Class<*>, HashMap<Class<*>, List<BaseAndroidScreenBinder>>>()
    private val hiddenBinders: ArrayList<BaseAndroidScreenBinder> = arrayListOf()

    var screenSettingsStack = Stack<ScreenSettings>()

    private lateinit var firstScreenSettings: ScreenSettings
    private var firstData: Any? = null

    private var isScreenShown = false

    var onBackPressed: (() -> Unit)? = null
    var onOptionsItemSelected: ((item: MenuItem) -> Boolean)? = null
    var onPrepareOptionsMenu: ((menu: Menu?) -> Boolean)? = null

    private var nextFragmentId = Long.MAX_VALUE

    private fun getBindersByScreen(screenCls: Class<*>): Collection<BaseAndroidScreenBinder> {
        return bindersByScreenClsMap[screenCls]
                ?: throw NullPointerException("screenCls:${screenCls.simpleName} | Array of binders must not be null!")
    }

    private fun getCurrentBindersByMainScreen(mainScreenCls: Class<*>): Collection<BaseAndroidScreenBinder> {
        if (screenSettingsStack.isEmpty()) {
            return emptyList()
        }

        val screens = getScreensByMainScreen(mainScreenCls)
        val allBinders = ArrayList<BaseAndroidScreenBinder>()
        screens.forEach { screen ->
            val bindersForScreen = getBindersByScreen(screen.javaClass)

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

    //NOTE: it is used in generated screens adapter
    protected fun createNotFoundException(name: String) =
            IllegalStateException("Invalid Screen!!! The \"$name\" screen is not found in the generator.")

    abstract fun generateBinders(screenCls: Class<*>): Collection<BaseAndroidScreenBinder>

    abstract fun generateScreens(mainScreenCls: Class<*>, data: Any?, parent: Any?): Collection<Any>

    fun restoreScreen(screenName: String): Any? {
        return screenName.getData()
    }

    // BaseFragment callbacks

    fun onFragmentStart(screenCls: Class<*>, fragmentId: Long) {
        Log.d(TAG, "     onFragmentStart: ${screenCls.simpleName}")
        val fragmentBindersById =
                getFragmentBindersByIdMap(screenCls, fragmentId)

        fragmentBindersById[fragmentId]?.forEach { binder ->
            binder.onShow()
        }
    }

    fun onFragmentFocus(screenCls: Class<*>, fragmentId: Long) {
        Log.d(TAG, "     onFragmentFocus: ${screenCls.simpleName}")
        val fragmentBindersById =
                getFragmentBindersByIdMap(screenCls, fragmentId)

        fragmentBindersById[fragmentId]?.forEach { binder ->
            binder.onFocus()
        }
    }

    fun onFragmentSelected(screenCls: Class<*>, fragmentId: Long) {
        Log.d(TAG, "     onFragmentSelected: ${screenCls.simpleName}")
        val fragmentBindersById =
                getFragmentBindersByIdMap(screenCls, fragmentId)

        fragmentBindersById[fragmentId]?.forEach { binder ->
            (binder as FragmentScreenBinder<*>).onSelectedInPager()
        }
    }

    fun onFragmentLostFocus(screenCls: Class<*>, fragmentId: Long) {
        Log.d(TAG, "     onFragmentLostFocus: ${screenCls.simpleName}")
        val fragmentBindersById =
                getFragmentBindersByIdMap(screenCls, fragmentId)

        fragmentBindersById[fragmentId]?.forEach { binder ->
            binder.onLostFocus()
        }
    }

    fun onFragmentStop(screenCls: Class<*>, fragmentId: Long) {
        Log.d(TAG, "     onFragmentStop: ${screenCls.simpleName}")
        val fragmentBindersById =
                getFragmentBindersByIdMap(screenCls, fragmentId)

        fragmentBindersById[fragmentId]?.forEach { binder ->
            binder.onHide()
        }
    }

    fun onFragmentCreate(screenCls: Class<*>, fragmentId: Long) {
        Log.d(TAG, "     onFragmentCreate: ${screenCls.simpleName}")
        val fragmentBindersById =
                getFragmentBindersByIdMap(screenCls, fragmentId)

        fragmentBindersById[fragmentId]?.forEach { binder ->
            binder.onBind()
        }
    }

    fun onFragmentDestroy(screenCls: Class<*>, fragmentId: Long) {
        fragmentBindersByScreenClsMap[screenCls.javaClass]?.clear()
    }

    private fun getFragmentBindersByIdMap(screenCls: Class<*>, fragmentId: Long): HashMap<Long, List<BaseAndroidScreenBinder>> {
        if (screenCls.name == "java.lang.Object" && fragmentId <= 0) {
            throw NullPointerException(createFragmentBindersExceptionData(screenCls, fragmentId) +
                    "NOTE: try to use  the screensAdapter.createFragment(...) method to create any fragment")
        }
        return fragmentBindersByScreenClsMap[screenCls]
                ?: throw NullPointerException(createFragmentBindersExceptionData(screenCls, fragmentId))
    }

    private fun createFragmentBindersExceptionData(screenCls: Class<*>, fragmentId: Long): String {
        return "data:\n" +
                "[ screenCls = ${screenCls.name} ]\n" +
                "[ fragmentId = $fragmentId ]\n"
    }

    // BaseActivity callbacks

    fun onStarterActivityCreated(activity: Activity) {
        Handler().postDelayed({
            Log.i(TAG, "show starter!")
            showNextScreen(firstScreenSettings, activity, data = firstData, parent = null)
            ActivityCompat.finishAfterTransition(activity)
        }, delayForSplashMs)
    }

    fun onActivityStart(activity: BaseActivity, screenCls: Class<*>) {
        val currentScreenCls = getCurrentScreenCls()
        if (currentScreenCls != null && currentScreenCls != screenCls) {
            return
        }

        Log.d(TAG, "onActivityStart: ${screenCls.simpleName}")
        isScreenShown = true
        currentActivityRef = WeakReference(activity)

        val binders = getBindersByScreen(screenCls)
        binders.forEach { binder ->
            binder.onShow()
        }

        callForAllChildBinders { binder ->
            binder.onShow()
        }
    }

    private fun callForAllChildBinders(call: (BaseAndroidScreenBinder) -> Unit) {
        val childBindersByChildScreenClsMap = childBindersByMainScreenClsMap[getCurrentScreenCls()]
        childBindersByChildScreenClsMap?.forEach { childBindersEntry ->
            val childBinders = childBindersEntry.value
            childBinders.forEach { binder ->
                call.invoke(binder)
            }
        }
    }

    fun onActivityResume(screenCls: Class<*>) {
        val currentScreenCls = getCurrentScreenCls()
        if (currentScreenCls != null && currentScreenCls != screenCls) {
            return
        }

        Log.d(TAG, "onActivityResume: ${screenCls.simpleName}")
        val binders = getCurrentBindersByMainScreen(screenCls)
        binders.forEach { binder ->
            binder.onFocus()
        }

        callForAllChildBinders { binder ->
            binder.onFocus()
        }
    }

    fun onActivityPause(screenCls: Class<*>) {
        Log.d(TAG, "onActivityPause: ${screenCls.simpleName}")
        val binders = hiddenBinders.takeIf { hiddenBinders.isNotEmpty() }
                ?: getCurrentBindersByMainScreen(screenCls)
        binders.forEach { binder ->
            binder.onLostFocus()
        }

        callForAllChildBinders { binder ->
            binder.onLostFocus()
        }
    }

    fun onActivityStop(activity: BaseActivity, screenCls: Class<*>) {
        Log.d(TAG, "onActivityStop: ${screenCls.simpleName}")
        val binders = hiddenBinders.takeIf { hiddenBinders.isNotEmpty() }
                ?: getCurrentBindersByMainScreen(screenCls)
        binders.forEach { binder ->
            binder.onHide()
        }

        callForAllChildBinders { binder ->
            binder.onHide()
        }

        hiddenBinders.clear()

        val isSameActivity = getCurrentActivity()?.equals(activity) == true
        if (isSameActivity) {
            isScreenShown = false
        }

        if (!isScreenShown) {
            getApp().onHideAllScreens(screenSettingsStack.size)
        }
    }

    fun onActivityCreateBeforeSuperCalled(activity: BaseActivity, screenCls: Class<*>) {
        Log.d(TAG, "onActivityCreateBeforeSuperCalled: ${screenCls.simpleName}")

        currentActivityRef = WeakReference(activity)
        val binders = getCurrentBindersByMainScreen(screenCls)
        binders.forEach { binder ->
            binder.onPrepare()
        }

        callForAllChildBinders { binder ->
            binder.onPrepare()
        }
    }

    fun onActivityCreated(activity: BaseActivity, screenCls: Class<*>) {
        Log.d(TAG, "onActivityCreated: ${screenCls.simpleName}")

        currentActivityRef = WeakReference(activity)
        val binders = getCurrentBindersByMainScreen(screenCls)
        binders.forEach { binder ->
            binder.onBind()
        }

        callForAllChildBinders { binder ->
            binder.onBind()
        }
    }

    fun onActivityResult(activity: BaseActivity, requestCode: Int, resultCode: Int, data: Intent?, screenCls: Class<*>) {
        Log.w(TAG, "onActivityResult: ${screenCls.simpleName} | requestCode = $requestCode | resultCode = $resultCode")

        isScreenShown = true
        currentActivityRef = WeakReference(activity)

        val binders = getCurrentBindersByMainScreen(screenCls)
        binders.forEach { binder ->
            binder.onActivityResult(requestCode, resultCode, data)
        }

        callForAllChildBinders { binder ->
            binder.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, screenCls: Class<*>) {
        Log.w(TAG, "onRequestPermissionsResult: ${screenCls.simpleName} | requestCode = $requestCode | grantResults = $grantResults")

        val binders = getCurrentBindersByMainScreen(screenCls)
        binders.forEach { binder ->
            binder.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

        callForAllChildBinders { binder ->
            binder.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

        setHiddenBinders(currentScreenSettings)

        bindersByScreenClsMap[currentScreenSettings.screenCls] = emptyList()

        val currentActivity = getCurrentActivity()
        currentActivity?.run {
            ActivityCompat.finishAfterTransition(this)
        }
    }

// There are available to use from binders:

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
        onBackPressed = null

        if (settings.finishAllPreviousScreens) {
            setHiddenBinders(getCurrentSettings())
            screenSettingsStack.clear()
        }

        val screens = generateScreens(settings.screenCls, data, parent)
//        screensByMainScreenClsMap.clear() //todo:
        screensByMainScreenClsMap[settings.screenCls] = screens

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
            setHiddenBinders(prevSettings)


            if (prevSettings.finishOnNextScreen || settings.finishPreviousScreen) {
                popScreen()
                bindersByScreenClsMap[prevSettings.screenCls] = emptyList()
            }
        }

        pushScreen(settings)
        startActivity(settings, prevSettings, activity)

        saveScreensAdapterState()
    }

    private fun saveScreensAdapterState() {
        //todo: Save and restore binders, screens and settings and firstData
        //todo: save all
    }

    private fun restoreScreensAdapterState() {
        //todo: restore all
    }

    private fun setHiddenBinders(currentScreenSettings: ScreenSettings) {
        val screenCls = currentScreenSettings.screenCls
        hiddenBinders.clear()

        val binders = bindersByScreenClsMap[screenCls]!!
        hiddenBinders.addAll(binders)

        val childBindersByChildScreenClsMap
                = childBindersByMainScreenClsMap[getCurrentScreenCls()]
        childBindersByChildScreenClsMap?.forEach { entry ->
            val childBinders = entry.value
            hiddenBinders.addAll(childBinders)
        }
    }

    private fun pushScreen(settings: ScreenSettings) {
        screenSettingsStack.push(settings)
    }

    private fun popScreen() = screenSettingsStack.pop()

    fun hideCurrentScreen() = backToPreviousScreenOrClose()

    fun getCurrentScreen(): Any {
        val screenCls = getCurrentScreenCls()
        return screensByMainScreenClsMap[screenCls]?.first()
                ?: throw NullPointerException("Screen must be not null!")
    }

    private fun getCurrentScreenCls() = screenSettingsStack.takeIf { it.size > 0 }
            ?.peek()?.screenCls

    fun getScreensByMainScreen(mainScreenCls: Class<*>): Collection<Any> {
        return screensByMainScreenClsMap[mainScreenCls]
                ?: throw NullPointerException("Init supportScreens for settings before use")
    }

    fun start() {
        val launchNumber = DATA.LAUNCH_NUMBER.getData() ?: 0
        getApp().onStart(launchNumber)

        DATA.LAUNCH_NUMBER.putData(launchNumber + 1)
    }

    fun resetToFirstScreen(data: Any? = firstData) {
        val firstScreenSettings = screenSettingsStack.last()
        firstScreenSettings.finishAllPreviousScreens = true
        showNextScreen(firstScreenSettings, data = data, parent = null)
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return onOptionsItemSelected?.invoke(item) ?: false
    }

    fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return onPrepareOptionsMenu?.invoke(menu) ?: true
    }

    fun getCurrentSettings(): ScreenSettings {
        if (screenSettingsStack.isEmpty()) {
            restoreScreensAdapterState()
        }

        return screenSettingsStack.peek()
    }
}
