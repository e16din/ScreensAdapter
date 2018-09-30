package com.e16din.screensadapter.processor

import com.e16din.screensadapter.annotation.AddSupportBinder
import com.e16din.screensadapter.annotation.BindScreen
import com.e16din.screensadapter.annotation.model.App
import com.e16din.screensadapter.annotation.model.Screen
import com.e16din.screensadapter.annotation.model.Server
import com.e16din.screensadapter.processor.ScreensAdapterProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.tools.Diagnostic


//@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ScreensAdapterProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        private const val SCREENS_ADAPTER_PACKAGE = "com.e16din.screensadapter"

        private const val PADDING_1 = "  "
        private const val PADDING_2 = "    "
        private const val PADDING_3 = "          "

        private val CLS_APPLICATION =
                ClassName("android.app", "Application")
        private val CLS_BASE_SCREEN_BINDER =
                ClassName("com.e16din.screensadapter.binders", "BaseScreenBinder")
    }

    private lateinit var filer: Filer
    private lateinit var messager: Messager

    private var generated = false

    private var appClassName: String? = null
    private var serverClassName: String? = null

    private var screensByMainScreenMap = HashMap<String, List<String>>()
    private var screensByBinderMap = HashMap<String, List<String>>()

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        val annotations = HashSet<String>()
        annotations.add(App::class.java.canonicalName)
        annotations.add(Server::class.java.canonicalName)
        annotations.add(Screen::class.java.canonicalName)
        annotations.add(BindScreen::class.java.canonicalName)
        annotations.add(AddSupportBinder::class.java.canonicalName)

        return annotations
    }

    @Synchronized
    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        filer = processingEnvironment.filer
        messager = processingEnvironment.messager
    }

    override fun process(set: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (generated) {
            return false
        }

        "process()".print()

        processApp(roundEnv)
        processServer(roundEnv)
        processBinders(roundEnv)
        processSupportBinders(roundEnv)

        if (appClassName != null
                && serverClassName != null) {

            generateScreensAdapter(SCREENS_ADAPTER_PACKAGE)
            generated = true
            return true
        }

        throw IllegalStateException("GeneratedScreensAdapter is empty")
    }

    private fun processServer(roundEnv: RoundEnvironment) {
        roundEnv.getElementsAnnotatedWith(Server::class.java)
                .firstOrNull()
                ?.run {
                    serverClassName = this.getFullName()
                    "serverClassName: $serverClassName".print()
                }
    }

    private fun processApp(roundEnv: RoundEnvironment) {
        roundEnv.getElementsAnnotatedWith(App::class.java)
                .firstOrNull()
                ?.run {
                    appClassName = this.getFullName()
                    "appClassName: $appClassName".print()
                }
    }

    private fun processBinders(roundEnv: RoundEnvironment) {
        ">> processBinders:".print()
        val binderElements =
                roundEnv.getElementsAnnotatedWith(BindScreen::class.java)

        binderElements.forEach { element ->
            val screenName = try {
                element.getAnnotation(BindScreen::class.java).screen.qualifiedName
            } catch (e: MirroredTypeException) {
                e.typeMirror.toString()
            }

            val annotationMirror = element.annotationMirrors.first()
            val mirrorAsStr = annotationMirror.toString()
            mirrorAsStr.print()
            val supportScreens = mirrorAsStr.substringBetween("supportScreens={", "}")
                    ?.split(", ")
                    ?.map { screenCls ->
                        screenCls.replace(".class", "")
                    }
            "supportScreens: ".print()
            supportScreens.toString().print()

            val screensForBinder = arrayListOf(screenName!!)
            supportScreens?.let {
                screensForBinder.addAll(it)
            }
            screensByMainScreenMap[screenName] = screensForBinder

            if (!isObjectClass(screenName)) {
                val binderName = element.getFullName()
                screensByBinderMap[binderName] = screensForBinder
            }
        }
        screensByBinderMap.forEach { binderName, screensNames ->
            "$binderName: $screensNames".print()
        }
    }

    private fun processSupportBinders(roundEnv: RoundEnvironment) {
        "processSupportBinders:".print()
        val addSupportBinderElements =
                roundEnv.getElementsAnnotatedWith(AddSupportBinder::class.java)

        addSupportBinderElements.forEach { element ->
            val binderName = element.getFullName()
            val annotationMirror = element.annotationMirrors.first()
            annotationMirror.toString().print()
            //Note: @com.e16din.screensadapter.annotation.BindScreens(screens={com.e16din.screensadapter.sample.screens.main.MainMapScreen.class, com.e16din.screensadapter.sample.screens.main.MainScreen.class})
            val screensClasses = arrayListOf<String>()

            val screensForBind = annotationMirror.toString()
                    .substringBetween("{", "}")
                    ?.split(", ")
                    ?.map { className ->
                        className.replace(".class", "")
                    } ?: arrayListOf()
            screensClasses.addAll(screensForBind)

            val screenForBindName = try {
                element.getAnnotation(AddSupportBinder::class.java).screen.qualifiedName
            } catch (e: MirroredTypeException) {
                e.typeMirror.toString()
            }!!
            if (!isObjectClass(screenForBindName)) {
                screensClasses.add(screenForBindName)
            }

            screensByBinderMap[binderName] = screensClasses

            screensClasses.forEach { screenName ->
                if (screensByMainScreenMap[screenName] == null) {
                    screensByMainScreenMap[screenName] = listOf(screenName)
                }
            }
        }
        screensByBinderMap.forEach { binderName, screensNames ->
            "$binderName: $screensNames".print()
        }
    }

    fun isObjectClass(className: String?): Boolean {
        return className!!.endsWith(".Object.class")
                || className.endsWith(".Object::class")
                || className.endsWith(".Object::class.java")
                || className.endsWith(".Object")
    }

    private fun Element.getFullName() =
            "${processingEnv.elementUtils.getPackageOf(this)}.${this.simpleName}"

    private fun generateScreensAdapter(pkg: String) {
        val fileName = "GeneratedScreensAdapter"

        val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                .orEmpty()
        if (generatedSourcesRoot.isEmpty()) {
            "Can't find the target directory for generated Kotlin files."
                    .printError()
            return
        }

        val file = File(generatedSourcesRoot).apply { mkdir() }

        val screensAdapterTypeSpec = TypeSpec.classBuilder(fileName)
                .addConstructor()
                .addGeneratedScreensFunc()
                .addGeneratedBindersFunc()
                .build()

        FileSpec.builder(pkg, fileName)
                .addType(screensAdapterTypeSpec)
                .build()
                .writeTo(file)
    }

    private fun (TypeSpec.Builder).addGeneratedBindersFunc(): TypeSpec.Builder {
        "addGeneratedBindersFunc:".print()

        val screenClsParamName = "screenCls"

        var bindersObjectsCode = ""

        screensByMainScreenMap.keys.forEach { mainScreenName ->
            val bindersForScreen = screensByBinderMap.keys.filter { binderName ->
                screensByBinderMap[binderName]!!.any { screenName ->
                    screenName == mainScreenName
                }
            }

            bindersObjectsCode += "$PADDING_2$mainScreenName::class.java -> arrayListOf(\n" +
                    bindersForScreen.joinToString(separator = ",\n") { binderName ->
                        "$PADDING_3$binderName(this)"
                    } +
                    "\n$PADDING_2)\n"
        }

        val funcSpec = FunSpec.builder("generateBinders")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(screenClsParamName, ClassName.bestGuess("Class<*>"))
                .addStatement(
                        "${PADDING_1}return when ($screenClsParamName) {\n" +
                                bindersObjectsCode +
                                "${PADDING_2}else -> throw createNotFoundException($screenClsParamName.simpleName)\n" +
                                "$PADDING_1}")
                .returns(ClassName.bestGuess("Collection").plusParameter(CLS_BASE_SCREEN_BINDER))
                .build()

        return this.addFunction(funcSpec)
    }

    private fun (TypeSpec.Builder).addGeneratedScreensFunc(): TypeSpec.Builder {
        val mainScreenClsParamName = "mainScreenCls"

        var mainScreensObjectsCode = ""
        screensByMainScreenMap.keys.forEach { mainScreenName ->
            mainScreensObjectsCode += "$PADDING_2$mainScreenName::class.java -> arrayListOf(\n" +
                    screensByMainScreenMap[mainScreenName]
                            ?.joinToString(separator = ",\n") { screenName ->
                                "$PADDING_3$screenName()"
                            } +
                    "$PADDING_2)\n"
        }

        val funcSpec = FunSpec.builder("generateScreens")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(mainScreenClsParamName, ClassName.bestGuess("Class<*>"))
                .addStatement(
                        "${PADDING_1}return when ($mainScreenClsParamName) {\n" +
                                mainScreensObjectsCode +
                                "${PADDING_2}else -> throw createNotFoundException($mainScreenClsParamName.simpleName)\n" +
                                "$PADDING_1}")
                .returns(ClassName.bestGuess("Collection<Any>"))
                .build()

        return this.addFunction(funcSpec)
    }

    private fun (TypeSpec.Builder).addConstructor(): TypeSpec.Builder {
        val superClassName = ClassName(SCREENS_ADAPTER_PACKAGE,
                "ScreensAdapter<$appClassName, $serverClassName>")

        val androidAppParamName = "androidApp"
        val appParamName = "app"
        val serverParamName = "server"
        val delayForSplashMsParamName = "delayForSplashMs"

        return this.superclass(superClassName).addModifiers(KModifier.PUBLIC)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter(androidAppParamName, CLS_APPLICATION)
                        .addParameter(appParamName, ClassName.bestGuess(appClassName!!))
                        .addParameter(serverParamName, ClassName.bestGuess(serverClassName!!))
                        .addParameter(delayForSplashMsParamName, Int::class)
                        .build())
                .addSuperclassConstructorParameter(androidAppParamName)
                .addSuperclassConstructorParameter(appParamName)
                .addSuperclassConstructorParameter(serverParamName)
                .addSuperclassConstructorParameter(delayForSplashMsParamName)
    }

    private fun String.print() =
            messager.printMessage(Diagnostic.Kind.WARNING, this)

    private fun String.printError() =
            messager.printMessage(Diagnostic.Kind.ERROR, this)
}

private fun String.substringBetween(start: String, end: String): String? {
    val startIndex = this.indexOf(start) + start.length
    val endIndex = this.indexOf(end)

    return try {
        this.substring(startIndex, endIndex)
    } catch (e: StringIndexOutOfBoundsException) {
        null
    }
}