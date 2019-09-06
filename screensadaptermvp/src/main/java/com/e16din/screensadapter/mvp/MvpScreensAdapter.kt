package com.e16din.screensadapter.mvp

import android.app.Application
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import com.e16din.screensadapter.ScreenSettings
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.mvp.activities.MvpBaseActivitySymbiont
import com.e16din.screensadapter.mvp.binders.IScreenBinder
import com.e16din.screensadapter.mvp.binders.android.FragmentScreenBinder
import com.e16din.screensadapter.mvp.fragments.BaseFragment
import com.e16din.screensadapter.mvp.helpers.addNow
import com.e16din.screensadapter.mvp.helpers.removeNow
import com.e16din.screensadapter.mvp.helpers.replaceNow
import com.e16din.screensadapter.mvp.model.BaseApp
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

//todo: Сделать @BindCase(caseCls, presenterCls) биндить автоматически на onBind
// смотри как навешиваются контролеры в ScreensController

abstract class MvpScreensAdapter<out APP : BaseApp, out SERVER>(
        androidApp: Application,
        appModel: APP,
        serverModel: SERVER) : ScreensAdapter(androidApp) {

    companion object {
        const val TAG = "SA.Mvp.Core"
    }

    private val androidAppRef = WeakReference(androidApp)

    private var appModelRef = WeakReference(appModel)
    private var serverModelRef = WeakReference<SERVER>(serverModel)

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

    var onOptionsItemSelectedListener: ((item: MenuItem) -> Boolean)? = null
    var onPrepareOptionsMenuListener: ((menu: Menu?) -> Boolean)? = null

    // ViewContainerId -> Stack<ScreenSettings>
    var addedFragmentsSettings = mutableMapOf<Int, Stack<ScreenSettings>>()

    // NOTE: Kowabunga!

    override fun backToPreviousScreenOrClose(withAnimation: Boolean, resultCode: Int?) {
        val currentScreenCls = getCurrentScreenCls()!!
        Log.i(TAG, "hideCurrentScreen: ${currentScreenCls.simpleName}")

        val binder = mainBindersMap[currentScreenCls]
        Log.d(TAG, "     ${binder!!.javaClass.simpleName}.onPrevScreen()")
        binder.onPrevScreen()

        callForActualChildBinders(currentScreenCls) { childBinder ->
            Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onPrevScreen()")
            childBinder.onPrevScreen()
        }

        super.backToPreviousScreenOrClose(withAnimation, resultCode)
    }

    private fun getCurrentScreenCls() = (items.lastOrNull() as MvpScreenSettings?)?.presenterCls

    // NOTE: There are used in generated screens adapter:

    protected fun createNotFoundException(name: String) =
            IllegalStateException("Invalid Screen!!! The \"$name\" screen is not found in the generator.")

    abstract fun makeBinder(screenCls: KClass<*>): IScreenBinder

    abstract fun getScreen(screenId: Int,
                           screenCls: KClass<*>?,
                           data: Any? = null,
                           parent: Any? = null,
                           recreate: Boolean): Any


    fun getAndroidApp() = androidAppRef.get()

    fun getApp() = appModelRef.get()!!
    fun getServer() = serverModelRef.get()!!

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
        binder.onBind()

        if (isScreenFocused) {
            binder.counter += 1
            binder.onShow()
            binder.onFocus()
        }
    }

    fun removeChildBinder(childScreenCls: KClass<*>, fragmentScreenId: Int = -1) {
        childBindersMap.remove(childScreenCls)
        if (fragmentScreenId >= 0) {
            fragmentChildBindersMap[fragmentScreenId]!!.remove(childScreenCls)
        }
    }

    fun createFragment(settings: MvpScreenSettings): BaseFragment {

        settings.screenId = generateScreenId(settings.screenId)
        fragmentChildBindersMap[settings.screenId] = ConcurrentHashMap()

        val screen = getScreen(
                settings.screenId,
                settings.presenterCls,
                settings.data,
                settings.parent,
                settings.recreateScreen)
        screensMap[settings.screenId] = Pair(settings.presenterCls, screen)

        Log.i(TAG, "fragmentId: ${settings.screenId}")
        val binder = makeBinder(screen.javaClass.kotlin)

        (binder as FragmentScreenBinder<*>).fragmentId = settings.screenId

        fragmentBindersMap[settings.screenId] = Pair(screen.javaClass.kotlin, binder)

        binder.initScreen(screen)

        return BaseFragment.create(settings.presenterCls,
                settings.layoutId!!,
                fragmentScreenId = settings.screenId)
    }

    fun showFragment(containerId: Int,
                     settings: MvpScreenSettings,
                     fragmentManager: FragmentManager = getCurrentActivity()?.supportFragmentManager!!,
                     addToBackStack: Boolean = false) {
        val fragment = createFragment(settings)
        val tag = "$containerId"

        val presenterClsName = settings.presenterCls.simpleName
        Log.i(TAG, "showFragmentScreen: $presenterClsName")

        if (addToBackStack) {
            if (addedFragmentsSettings[containerId].isNullOrEmpty()) {
                addedFragmentsSettings[containerId] = Stack()
            }
            addedFragmentsSettings[containerId]?.push(settings)

            fragmentManager.addNow(containerId, fragment, tag, presenterClsName)

        } else {
            addedFragmentsSettings[containerId]?.clear()

            val previousFragment = (fragmentManager.findFragmentByTag(tag) as BaseFragment?)
            previousFragment?.let {
                finishedFragmentsIds.add(it.fragmentId)
            }

            fragmentManager.replaceNow(containerId, fragment, tag)
        }
    }

    fun showFragmentFromStack(presenterCls: KClass<*>,
                              fragmentManager: FragmentManager = getCurrentActivity()?.supportFragmentManager!!) {
        // todo: показать фрагмент из стека
    }

    fun showPreviousFragmentFromStack(containerId: Int,
                                      fragmentManager: FragmentManager = getCurrentActivity()?.supportFragmentManager!!,
                                      immediate: Boolean = false) {

        if (addedFragmentsSettings.isEmpty()) {
            return
        }

        if (addedFragmentsSettings[containerId]?.isNotEmpty() == true) {
            addedFragmentsSettings[containerId]?.pop()
        }

        val fragment = fragmentManager.fragments.last() as BaseFragment
        finishedFragmentsIds.add(fragment.fragmentId)

        if (immediate) {
            fragmentManager.popBackStackImmediate()

        } else {
            fragmentManager.popBackStack()
        }
    }

    fun removeFragment(containerId: Int,
                       fragment: BaseFragment,
                       fragmentManager: FragmentManager = getCurrentActivity()?.supportFragmentManager!!) {

        addedFragmentsSettings[containerId]

        finishedFragmentsIds.add(fragment.fragmentId)
        fragmentManager.removeNow(fragment)
    }

    fun removeFragment(containerId: Int,
                       fragmentManager: FragmentManager = getCurrentActivity()?.supportFragmentManager!!) {
        val fragment = getCurrentActivity()?.supportFragmentManager
                ?.findFragmentByTag("$containerId")
        fragment?.let {
            removeFragment(containerId, it as BaseFragment, fragmentManager)
        }
    }

    override fun prepareScreen(settings: ScreenSettings) {
        super.prepareScreen(settings)
        settings as MvpScreenSettings

        val nextScreenCls = settings.presenterCls
        childBindersMap[nextScreenCls] = ConcurrentHashMap()

        val screen = getScreen(
                settings.screenId,
                nextScreenCls,
                settings.data,
                settings.parent,
                settings.recreateScreen)
        screensMap[settings.screenId] = Pair(nextScreenCls, screen)

        Log.i(TAG, "showNextScreen: ${screen.javaClass.simpleName}")
        val binder = makeBinder(screen.javaClass.kotlin)
        mainBindersMap[screen.javaClass.kotlin] = binder

        binder.initScreen(screen)
    }


    override fun beforeNextActivityStart(settings: ScreenSettings) {
        val mainScreenCls = (settings as MvpScreenSettings).presenterCls
        Log.d(TAG, "${mainScreenCls.simpleName}.beforeNextActivityStart()")
        val binder = mainBindersMap[mainScreenCls]
        binder!!.onNextScreen()

        callForActualChildBinders(mainScreenCls) { childBinder ->
            Log.d(TAG, "     ${childBinder.javaClass.simpleName}.beforeNextActivityStart()")
            childBinder.onNextScreen()
        }
    }

    fun getMvpSettingsPosition(presenterCls: KClass<*>) =
            items.indexOfLast { (it as MvpScreenSettings).presenterCls == presenterCls }

    override fun createActivitySymbiont() = MvpBaseActivitySymbiont()
}
