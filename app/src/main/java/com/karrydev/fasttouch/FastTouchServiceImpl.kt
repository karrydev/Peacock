package com.karrydev.fasttouch

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.Data
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ServiceLifecycleDispatcher
import com.google.gson.Gson
import com.karrydev.fasttouch.base.Utils
import com.karrydev.fasttouch.base.appContext
import com.karrydev.fasttouch.base.isDebug
import com.karrydev.fasttouch.broadcast.PackageChangeReceiver
import com.karrydev.fasttouch.broadcast.UserPresentReceiver
import com.karrydev.fasttouch.databinding.LayoutCustomizationDialogBinding
import com.karrydev.fasttouch.model.PackagePositionDescription
import com.karrydev.fasttouch.model.PackageWidgetDescription
import com.karrydev.fasttouch.model.Settings
import java.util.Date
import java.util.Deque
import java.util.LinkedList
import java.util.TreeMap
import java.util.concurrent.*
import kotlin.math.roundToInt

class FastTouchServiceImpl(private val service: AccessibilityService) {

    private val TAG = "FastTouchServiceImpl"
    private val selfPkgName = "开屏跳过"
    private lateinit var curPkgName: String
    private lateinit var curActName: String

    @Volatile
    private var skipAdTaskRunning = false
    @Volatile
    private var skipAdByActivityPosition = false
    @Volatile
    private var skipAdByActivityWidget = false
    @Volatile
    private var skipAdByKeyword = false

    /**
     * 当前应用的包名
     */
    private lateinit var packageName: String
    private lateinit var packageManager: PackageManager
    /**
     * 所有可跳过的包集合
     */
    private lateinit var setPackages: HashSet<String>
    /**
     * 输入法App集合
     */
    private lateinit var setIMEApps: HashSet<String>
    /**
     * 白名单集合
     */
    private lateinit var setWhiteList: HashSet<String>
    private lateinit var mapPackagePositions: TreeMap<String, PackagePositionDescription>
    private lateinit var mapPackageWidgets: TreeMap<String, Set<PackageWidgetDescription>>
    @Volatile
    private var setTargetedWidgets: Set<PackageWidgetDescription>? = null

    private lateinit var packageChangeReceiver: PackageChangeReceiver
    private lateinit var userPresentReceiver: UserPresentReceiver

    private lateinit var settings: Settings
    private lateinit var keywordList: ArrayList<String>
    private lateinit var clickedWidgets: HashSet<String>
    private lateinit var skipAdExecutorService: ScheduledExecutorService
    lateinit var mainHandler: Handler

    private var customizationDialogShowing = false
    private lateinit var customizationDialogView: View
    private lateinit var dialogBinding: LayoutCustomizationDialogBinding
    private lateinit var frameOutlineLayout: FrameLayout
    private lateinit var imgPositionTarget: ImageView

    companion object {
        // 第一次在300ms以后，每500ms再点击一次，总共尝试5次点击
        const val PACKAGE_POSITION_CLICK_FIRST_DELAY = 300L
        const val PACKAGE_POSITION_CLICK_RETRY_INTERVAL = 500L
        const val PACKAGE_POSITION_CLICK_RETRY_TIME = 6
    }

    fun onServiceConnected() {
        // 初始化参数
        curPkgName = "Init PackageName"
        curActName = "Init PackageName"

        packageName = service.packageName

        // 从 SharedPreferences 获取 settings
        settings = Settings

        // 获取 keywords
        keywordList = settings.keywordList

        // 获取白名单
        setWhiteList = settings.whiteList

        // 找到所有的包
        packageManager = service.packageManager
        findAllPackages()

        // 加载用户添加的 坐标 和 控件
        mapPackagePositions = settings.mapPackagePositions
        mapPackageWidgets = settings.mapPackageWidgets

        // 初始化主线程 Handler 和 Receiver
        initReceiverAndHandler()

        // 初始化已点击的 widget 集合
        clickedWidgets = HashSet()

        // 创建线程池用于执行任务服务
        skipAdExecutorService = Executors.newSingleThreadScheduledExecutor()
//        skipAdExecutorService = Executors.newScheduledThreadPool(8)


        // 初始化用户自定义添加弹窗相关
        initCustomizationDialog()
    }

    private fun initReceiverAndHandler() {
        // 启动【应用安装】和【应用卸载】的广播接收器
        userPresentReceiver = UserPresentReceiver()
        service.registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))

        // 启动【设备唤醒】的广播接收器
        packageChangeReceiver = PackageChangeReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
        }
        service.registerReceiver(packageChangeReceiver, intentFilter)

        // 初始化 mainHandler 来处理 broadcast 的消息
        mainHandler = Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                FastTouchService.ACTION_REFRESH_KEYWORDS -> { // 更新 keywords
                    keywordList = settings.keywordList
                }
                FastTouchService.ACTION_REFRESH_PACKAGE -> { // 更新 package 列表
                    // 这里可能是用户设置【白名单】发生了变化，也可能是有【新应用安装】或发生【应用卸载】
                    setWhiteList = settings.whiteList
                    findAllPackages()
                }
                FastTouchService.ACTION_REFRESH_CUSTOMIZED_WIDGETS_POSITIONS -> { // 更新用户自定义跳过的内容
                    mapPackagePositions = settings.mapPackagePositions
                    mapPackageWidgets = settings.mapPackageWidgets
                }
                FastTouchService.ACTION_STOP_SERVICE -> { // 关闭无障碍服务
                    service.disableSelf()
                }
                FastTouchService.ACTION_SHOW_CUSTOMIZATION_DIALOG -> { // 打开用户自定义跳过方法弹窗
                    if (!customizationDialogShowing) {
                        showCustomizationDialog()
                    }
                }
                FastTouchService.ACTION_START_TASK -> { // 开启 skip-ad 任务
                    startSkipAdTask()
                }
                FastTouchService.ACTION_STOP_TASK -> { // 结束 skip-ad 任务
                    stopSkipAdTaskInner()
                }
            }
            true
        }
    }

    /**
     * 点击桌面应用图标后的事件顺序
     * TYPE_VIEW_CLICKED - net.oneplus.launcher - android.widget.TextView
     * TYPE_WINDOWS_CHANGED - null - null
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - android.widget.FrameLayout
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - android.widget.FrameLayout
     * TYPE_WINDOW_STATE_CHANGED - tv.danmaku.bili - android.widget.FrameLayout
     * TYPE_WINDOW_CONTENT_CHANGED - net.oneplus.launcher - android.widget.FrameLayout
     * TYPE_WINDOW_CONTENT_CHANGED - net.oneplus.launcher - android.widget.FrameLayout
     * TYPE_WINDOW_CONTENT_CHANGED - net.oneplus.launcher - android.widget.FrameLayout
     * TYPE_WINDOW_CONTENT_CHANGED - net.oneplus.launcher - android.widget.FrameLayout
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - android.widget.FrameLayout
     * TYPE_WINDOW_STATE_CHANGED - tv.danmaku.bili - tv.danmaku.bili.ui.splash.SplashActivity
     * TYPE_WINDOWS_CHANGED - null - null
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - android.widget.FrameLayout
     * TYPE_WINDOW_STATE_CHANGED - tv.danmaku.bili - tv.danmaku.bili.MainActivityV2
     * TYPE_WINDOWS_CHANGED - null - null
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - android.view.ViewGroup
     * TYPE_VIEW_SCROLLED - tv.danmaku.bili - android.widget.HorizontalScrollView
     * TYPE_WINDOWS_CHANGED - null - null
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - android.view.ViewGroup
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - android.widget.ImageView
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - androidx.viewpager.widget.ViewPager
     * TYPE_WINDOWS_CHANGED - null - null
     * TYPE_NOTIFICATION_STATE_CHANGED - tv.danmaku.bili - android.widget.Toast$TN
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - androidx.viewpager.widget.ViewPager
     * TYPE_VIEW_SCROLLED - tv.danmaku.bili - androidx.recyclerview.widget.RecyclerView
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - android.widget.ImageView
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - androidx.viewpager.widget.ViewPager
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - androidx.recyclerview.widget.RecyclerView
     * TYPE_WINDOW_CONTENT_CHANGED - tv.danmaku.bili - androidx.recyclerview.widget.RecyclerView
     * TYPE_WINDOW_CONTENT_CHANGED - com.android.systemui - android.widget.FrameLayout
     *
     * 无障碍服务功能的具体事件实现
     * 主要针对两个状态进行处理
     * [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED]
     * 表示打开弹出窗口、菜单、对话框等的事件。
     * 判断 packageName 和 activityName，决定是否开始跳过检测
     * 使用三种方法去尝试跳过【坐标跳过】【控件跳过】【关键词跳过】
     *
     * [AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED]
     * 表示窗口内容更改的事件。这种改变可以是添加/删除视图，改变视图大小，等等。
     * 使用两种方法去尝试跳过【控件跳过】【关键词跳过】
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName.toString() // 包名
        val className = event.className.toString() // 类名
        if (packageName.isEmpty() || className.isEmpty()) return


        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (setIMEApps.contains(packageName)) {
                    // 忽略输入法App，它可能会短暂启动
                    return
                }
                // 先判断当前的 pkg 是不是新的包以及是不是一个 Activity
                // 是新 pkg && 是新 Activity -> 记录当前 pkg 和 class，如果 pkg 在跳过列表内，则启动跳过任务
                // 不是新 pkg && 是新 Activity -> 记录当前 class
                val isActivity = !className.startsWith("android.") && !className.startsWith("androidx.")
                if (curPkgName != packageName) {
                    // 是新包
                    if (isActivity) {
                        // 是一个 Activity
                        curPkgName = packageName
                        curActName = className

                        // 如果当前有任务的话，先结束
                        stopSkipAdTask()
                        // 开启一个新任务
                        startSkipAdTask()
                    } else {
                        return
                    }
                } else {
                    // 不是新包
                    if (isActivity) {
                        // 是一个 Activity
                        if (curActName != className) {
                            // 老包下的一个新 Activity，有时候 Ad-Activity 并不是第一个 Activity
                            // 在这个包刚创建的时候已经启动了 skip 任务，有一定的检测时间，所以这里不进行额外操作
                            // TODO 如果用户设置了很短的检测时间这里就无法 skip，因为不能每一个新的 Activity 都启动一个新任务
                            curActName = className
                            return
                        }
                    } else {
                        return
                    }
                }

                // 走到这就代表当前页面需要 skip ads

                // 根据不同方法跳过广告：1、用户指定了坐标或某个控件；2、根据关键词跳过
                // 如果用户对某个 pkg 有指定坐标或控件，则执行【坐标跳过】或【控件跳过】，【关键词跳过】是兜底操作为必须执行项

                // 【坐标跳过】
                if (skipAdByActivityPosition) {
                    // 避免重复执行
                    skipAdByActivityPosition = false

                    val packagePositionDescription = mapPackagePositions[curPkgName]
                    if (packagePositionDescription != null) {
                        showToastHandler("正在根据位置跳过广告...")

                        // 尝试多次点击指定位置
                        var num = 0
                        var futures: ScheduledFuture<*>? = null
                        futures = skipAdExecutorService.scheduleAtFixedRate({
                            if (num < PACKAGE_POSITION_CLICK_RETRY_TIME) {
                                if (curActName == packagePositionDescription.activityName) {
                                    simulatedClick(packagePositionDescription.x, packagePositionDescription.y , 0, 40)
                                }
                                num++
                            } else {
                                futures?.cancel(true)
                            }
                        }, PACKAGE_POSITION_CLICK_FIRST_DELAY, PACKAGE_POSITION_CLICK_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                    }
                }

                // 【控件跳过】
                if (skipAdByActivityWidget) {
                    // 只需要查找一次
                    skipAdByActivityWidget = false
                    // 获取到用户指定的控件
                    setTargetedWidgets = mapPackageWidgets[curPkgName]
                }
                if (setTargetedWidgets != null) {
                    // 这个代码块可能会执行多次
                    val node = service.rootInActiveWindow
                    val widgets = setTargetedWidgets
                    skipAdExecutorService.execute { iterateNodesToSkipAd(node, widgets) }
                }

                // 【关键词跳过】可能会执行多次
                val node = service.rootInActiveWindow
                skipAdExecutorService.execute { iterateNodesToSkipAd(node) }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (!setPackages.contains(packageName)) return

                val node = event.source
                if (node != null) {
                    // 【控件跳过】
                    if (setTargetedWidgets != null) {
                        val widget = setTargetedWidgets
                        skipAdExecutorService.execute { iterateNodesToSkipAd(node, widget) }
                    }

                    // 【关键词跳过】
                    if (skipAdByKeyword) {
                        skipAdExecutorService.execute { iterateNodesToSkipAd(node) }
                    }
                }
            }
        }
    }

    fun onUnbind(intent: Intent?) {
        service.unregisterReceiver(packageChangeReceiver)
        service.unregisterReceiver(userPresentReceiver)
    }

    /**
     * 遍历节点跳过广告
     * 如果 set 为空时按 keyword 判断，否则按 set 内控件判断
     * @param root 根节点
     * @param set 控件集合
     */
    private fun iterateNodesToSkipAd(root: AccessibilityNodeInfo, set: Set<PackageWidgetDescription>? = null) {
        val pkgName = root.packageName.toString()
        if (!setPackages.contains(pkgName)) return

        val nodeQueue: Deque<AccessibilityNodeInfo> = LinkedList()
        nodeQueue.offer(root)

        var node : AccessibilityNodeInfo
        var skipFlag : Boolean
        // 通过广度优先遍历的方式进行 ViewTree 的遍历
        while (nodeQueue.isNotEmpty() && skipAdTaskRunning) {
            node = nodeQueue.pop()
            skipFlag = if (set == null) {
                skipByKeywords(node)
            } else {
                skipByTargetedWidget(node, set)
            }

            // 如果跳过成功则结束遍历，否则将 node 的 childNode 添加进遍历列表
            if (skipFlag) {
                break
            }
            for (i in 0 until node.childCount) {
                nodeQueue.offer(node.getChild(i))
            }
        }
    }

    /**
     * 查找并点击包含 keyword 的控件，包括 Text 和 Description
     */
    private fun skipByKeywords(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()
        val description = node.contentDescription?.toString()
        if (text.isNullOrEmpty() && description.isNullOrEmpty()) return false

        // 尝试寻找 keyword
        var isFound = false
        keywordList.forEach { keyword ->
            // text 或者 description 包含 keyword，且不能太长（这里设置了 keyword.length+6 为了避免原本是一段正常的文字包含了 keyword 的情况）
            if (text != null && text.contains(keyword) && !text.contains(selfPkgName) && (text.length <= keyword.length + 6) ||
                description != null && description.contains(keyword) && !description.contains(selfPkgName) && (description.length <= keyword.length + 6)) {
                isFound = true
            }
            if (isFound) {
                // 此时已经找到对应的 node，可以结束循环
                return@forEach
            }
        }
        // 如果找到某个 node 则尝试点击它
        if (isFound) {
            val nodeDesc = Utils.describeAccessibilityNode(node)
            if (!clickedWidgets.contains(nodeDesc)) {
                // 避免重复点击
                clickedWidgets.add(nodeDesc)

                showToastHandler("正在根据关键字跳过广告...")

                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (!clicked) {
                    // 如果 ACTION_CLICK 不生效则进行模拟点击
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    simulatedClick(rect.centerX(), rect.centerY(), 0, 20)
                }

                return true
            }
        }

        return false
    }

    /**
     * 模拟点击
     */
    private fun simulatedClick(x: Int, y: Int, startTime: Long, duration: Long): Boolean {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val build = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, startTime, duration))
            .build()
        return service.dispatchGesture(build, null, null)
    }

    /**
     * 查找并点击用户指定的控件
     */
    private fun skipByTargetedWidget(node: AccessibilityNodeInfo, set: Set<PackageWidgetDescription>): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        val id = node.viewIdResourceName
        val text = node.text?.toString()
        val description = node.contentDescription?.toString()
        set.forEach { pwd ->
            // 判断当前 node 是否在用户指定的控件集合内
            val isFound =
                rect == pwd.rect ||
                id != null && pwd.idName.isNotEmpty() && id.contains(pwd.idName) ||
                text != null && pwd.text.isNotEmpty() && text.contains(pwd.text) ||
                description != null && pwd.description.isNotEmpty() && description.contains(pwd.description)

            if (isFound) {
                // 找到了用户指定的控件
                val nodeDesc = Utils.describeAccessibilityNode(node)

                if (!clickedWidgets.contains(nodeDesc)) {
                    // 避免重复点击
                    clickedWidgets.add(nodeDesc)

                    showToastHandler("正在根据控件跳过广告...")

                    if (pwd.onlyClick) {
                        simulatedClick(rect.centerX(), rect.centerY(), 0 ,20)
                    } else {
                        if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            if (!node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                simulatedClick(rect.centerX(), rect.centerY(), 0, 20)
                            }
                        }
                    }

                    // 清空该集合引用，避免后续重复使用
                    if (setTargetedWidgets == set) setTargetedWidgets = null
                    return true
                }
            }
        }

        return false
    }

    /**
     * 找到所有的包，添加包/删除包也会触发该方法
     */
    private fun findAllPackages() {
        setPackages = HashSet()
        setIMEApps = HashSet()
        val setTemps = HashSet<String>()

        // 找到所有启动器
        var intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        var resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolveInfoList.forEach { info ->
            setPackages.add(info.activityInfo.packageName)
        }

        // 找到桌面
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        resolveInfoList.forEach { info ->
            setTemps.add(info.activityInfo.packageName)
        }

        // 找到所有输入法
        val inputMethodInfoList = (service.getSystemService(AccessibilityService.INPUT_METHOD_SERVICE) as InputMethodManager).inputMethodList
        inputMethodInfoList.forEach { info ->
            setIMEApps.add(info.packageName)
        }

        // 忽略一些特定的包
        setTemps.add(packageName)
        setTemps.add("com.android.settings")

        // 移除掉所有白名单应用、系统应用、桌面以及一些特殊的包，最终得到的就是可以进行开屏跳过的包集合 setPackages
        setPackages.apply {
            removeAll(setWhiteList)
            removeAll(setIMEApps)
            removeAll(setTemps)
        }
    }

    /**
     * 初始化用户自定义添加弹窗相关
     */
    private fun initCustomizationDialog() {
        // 创建弹窗布局
        val inflater = LayoutInflater.from(service)
        customizationDialogView = inflater.inflate(R.layout.layout_customization_dialog, null)
        dialogBinding = DataBindingUtil.bind(customizationDialogView)!!

        // 创建【显示布局】功能的顶层 frame
        frameOutlineLayout = inflater.inflate(R.layout.layout_description_layout_frame, null)
            .findViewById(R.id.frame)

        // 创建准心图片
        imgPositionTarget = ImageView(service)
        imgPositionTarget.setImageResource(R.drawable.ic_target)
    }

    /**
     * 查找 root 中所有的控件
     */
    private fun findAllNode(root: ArrayList<AccessibilityNodeInfo>, list: ArrayList<AccessibilityNodeInfo>) {
        val childrenList = ArrayList<AccessibilityNodeInfo>()
        root.forEach { node ->
            list.add(node)
            for (i in 0 until node.childCount) {
                childrenList.add(node.getChild(i))
            }
        }
        if (childrenList.isNotEmpty()) {
            findAllNode(childrenList, list)
        }
    }

    /**
     * 遍历 root 的布局，并记录每一个控件的信息
     */
    private fun dumpRootNode(root: AccessibilityNodeInfo): String {
        val nodeList = ArrayList<AccessibilityNodeInfo>()
        val dumpString = StringBuilder()
        dumpChildNodes(root, nodeList, dumpString, "")
        return dumpString.toString()
    }

    /**
     * 通过递归获取控件信息
     */
    private fun dumpChildNodes(root: AccessibilityNodeInfo?, list: ArrayList<AccessibilityNodeInfo>, dumpString: StringBuilder, indent: String) {
        if (root == null) return
        list.add(root)
        dumpString.append(indent + Utils.describeAccessibilityNode(root) + "\n")

        for (i in 0 until root.childCount) {
            val child = root.getChild(i)
            dumpChildNodes(child, list, dumpString, " ")
        }
    }

    /**
     * 展示用户自定义弹窗，用户通过该弹窗可以选择页面想要点击的 widgets 或者 positions
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun showCustomizationDialog() {
        // 记录弹窗状态，避免开启多个弹窗
        customizationDialogShowing = true

        val windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val b = metrics.heightPixels > metrics.widthPixels
        val width = if (b) metrics.widthPixels else metrics.heightPixels
        val height = if (b) metrics.heightPixels else metrics.heightPixels

        // 用来记录被选择的 widget 或 position 的信息
        val widgetDescription = PackageWidgetDescription()
        val positionDescription = PackagePositionDescription()

        // 弹窗视图参数
        val dialogParams = WindowManager.LayoutParams()
        dialogParams.apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSPARENT
            gravity = Gravity.START or Gravity.TOP
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            this.width = width
            this.height = height / 5
            x = (metrics.widthPixels - this.width) / 2
            y = metrics.heightPixels - this.height
            alpha = 0.8f
        }

        // 【显示布局】中的轮廓视图参数
        val frameOutlineParams = WindowManager.LayoutParams()
        frameOutlineParams.apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSPARENT
            gravity = Gravity.START or Gravity.TOP
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            this.width = metrics.widthPixels
            this.height = metrics.heightPixels
            alpha = 0f
        }

        // 准心图片视图参数
        val positionTargetParams = WindowManager.LayoutParams()
        positionTargetParams.apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSPARENT
            gravity = Gravity.START or Gravity.TOP
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            this.width = width / 4
            this.height = width / 4
            x = (metrics.widthPixels - this.width) / 2
            y = (metrics.heightPixels - this.height) / 2
            alpha = 0f
        }

        // 添加布局，添加顺序不能错
        windowManager.addView(frameOutlineLayout, frameOutlineParams)
        windowManager.addView(customizationDialogView, dialogParams)
        windowManager.addView(imgPositionTarget, positionTargetParams)

        // 实现弹窗的拖动，这里可能可以忽略X轴
        customizationDialogView.setOnTouchListener(object: View.OnTouchListener {
//            var x = 0
            var y = 0

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event != null) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
//                            x = event.rawX.roundToInt()
                            y = event.rawY.roundToInt()
                        }
                        MotionEvent.ACTION_MOVE -> {
//                            dialogParams.x = (dialogParams.x + event.rawX - x).roundToInt()
                            dialogParams.y = (dialogParams.y + event.rawY - y).roundToInt()
//                            x = event.rawX.roundToInt()
                            y = event.rawY.roundToInt()
                            windowManager.updateViewLayout(customizationDialogView, dialogParams)
                        }
                    }
                }
                return true
            }
        })

        // 【显示布局】
        dialogBinding.btnShowOutline.setOnClickListener {
            val btn = it as Button
            // 根据 frame 的是否透明来确定是否开启了该功能
            if (frameOutlineParams.alpha == 0f) {
                // 通过一个透明的图层根据当前页面的控件绘制出控件框展示给用户
                val root = service.rootInActiveWindow ?: return@setOnClickListener
                widgetDescription.packageName = curPkgName
                widgetDescription.activityName = curActName
                frameOutlineLayout.removeAllViews()

                // 获取 root 下所有子控件
                val roots = ArrayList<AccessibilityNodeInfo>()
                roots.add(root)
                val nodeList = ArrayList<AccessibilityNodeInfo>()
                findAllNode(roots, nodeList)
                nodeList.sortWith { a, b ->
                    // 根据控件大小来进行升序排序
                    val rectA = Rect()
                    val rectB = Rect()
                    a.getBoundsInScreen(rectA)
                    b.getBoundsInScreen(rectB)
                    return@sortWith rectB.width() * rectB.height() - rectA.width() * rectA.height()
                }

                nodeList.forEach { node ->
                    // 根据 node 的大小创建相应的布局
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    val params = FrameLayout.LayoutParams(rect.width(), rect.height())
                    params.leftMargin = rect.left
                    params.topMargin = rect.top

                    val img = ImageView(service)
                    img.apply {
                        setBackgroundResource(R.drawable.node)
                        isFocusableInTouchMode = true
                        setOnClickListener { v -> v.requestFocus() }
                        setOnFocusChangeListener { v, hasFocus ->
                            if (hasFocus) {
                                // 绘制出控件框
                                widgetDescription.apply {
                                    this.rect = rect
                                    clickable = node.isClickable
                                    className = if (node.className != null) node.className.toString() else ""
                                    idName = node.viewIdResourceName ?: ""
                                    description = if (node.contentDescription != null) node.contentDescription.toString() else ""
                                    text = if (node.text != null) node.text.toString() else ""

                                    dialogBinding.apply {
                                        btnAddWidget.isEnabled = true
                                        tvPackageName.text = packageName
                                        tvActivityName.text = activityName
                                        val idStr = if (idName.indexOf("id/") + 3 < idName.length) idName.substring(idName.indexOf("id/") + 3) else ""
                                        tvWidgetInfo.text = "click:${node.isClickable}  bonus:${rect.toShortString()}\nid:$idStr  desc:$description  text:$text"
                                    }
                                }
                                // 添加选中背景色
                                v.setBackgroundResource(R.drawable.node_focus)
                            } else {
                                // 删除选中背景色
                                v.setBackgroundResource(R.drawable.node)
                            }
                        }
                    }

                    frameOutlineLayout.addView(img, params)
                }

                frameOutlineParams.alpha = 0.5f
                frameOutlineParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                // TODO 这里的frameOutlineLayout可能有问题
                windowManager.updateViewLayout(frameOutlineLayout, frameOutlineParams)
                dialogBinding.tvPackageName.text = widgetDescription.packageName
                dialogBinding.tvActivityName.text = widgetDescription.activityName
                btn.text = "隐藏布局"
            } else {
                // 隐藏布局
                frameOutlineParams.alpha = 0f
                frameOutlineParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                windowManager.updateViewLayout(frameOutlineLayout, frameOutlineParams)
                dialogBinding.btnAddWidget.isEnabled = false
                btn.text = "显示布局"
            }
        }

        // 【添加控件】
        dialogBinding.btnAddWidget.setOnClickListener {
            val widgetDesc = PackageWidgetDescription(widgetDescription)
            var set = mapPackageWidgets[widgetDescription.packageName] as HashSet?
            if (set == null) {
                set = HashSet()
                set.add(widgetDesc)
                mapPackageWidgets[widgetDescription.packageName] = set
            }
            dialogBinding.btnAddWidget.isEnabled = false
            dialogBinding.tvPackageName.text = "${widgetDescription.packageName} (以下控件数据已保存)"

            // 更新设置，记录用户选中的控件
            settings.mapPackageWidgets = mapPackageWidgets
        }

        // 【显示准心】
        dialogBinding.btnShowTarget.setOnClickListener {
            val btn = it as Button
            // 根据 image 的是否透明来确定是否开启了该功能
            if (positionTargetParams.alpha == 0f) {
                positionDescription.packageName = curPkgName
                positionDescription.activityName = curActName
                positionTargetParams.alpha = 0.5f
                positionTargetParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(imgPositionTarget, positionTargetParams)
                dialogBinding.tvPackageName.text = positionDescription.packageName
                dialogBinding.tvActivityName.text = positionDescription.activityName
                btn.text = "隐藏准心"
            } else {
                // 隐藏准心
                positionTargetParams.alpha = 0f
                positionTargetParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                windowManager.updateViewLayout(imgPositionTarget, positionTargetParams)
                dialogBinding.btnAddPosition.isEnabled = false
                btn.text = "显示准心"
            }
        }

        // 实现准心图片的拖动
        imgPositionTarget.setOnTouchListener(object: View.OnTouchListener {
            var x = 0
            var y = 0
            var targetWidth = positionTargetParams.width / 2
            var targetHeight = positionTargetParams.height / 2

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event != null) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            dialogBinding.btnAddPosition.isEnabled = true
                            positionTargetParams.alpha = 0.9f
                            windowManager.updateViewLayout(imgPositionTarget, positionTargetParams)

                            x = event.rawX.roundToInt()
                            y = event.rawY.roundToInt()
                        }
                        MotionEvent.ACTION_MOVE -> {
                            positionTargetParams.x = (positionTargetParams.x + event.rawX - x).roundToInt()
                            positionTargetParams.y = (positionTargetParams.y + event.rawY - y).roundToInt()
                            x = event.rawX.roundToInt()
                            y = event.rawY.roundToInt()
                            windowManager.updateViewLayout(imgPositionTarget, positionTargetParams)

                            positionDescription.apply {
                                packageName = curPkgName
                                activityName = curActName
                                x = positionTargetParams.x + targetWidth
                                y = positionTargetParams.y + targetHeight
                                dialogBinding.tvPackageName.text = packageName
                                dialogBinding.tvActivityName.text = activityName
                                dialogBinding.tvPositionInfo.text = "X轴：$x  Y轴：$y   （其它参数默认）"
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            positionTargetParams.alpha = 0.5f
                            windowManager.updateViewLayout(imgPositionTarget, positionTargetParams)
                        }
                    }
                }
                return true
            }
        })

        // 【添加坐标】
        dialogBinding.btnAddPosition.setOnClickListener {
            if (!setPackages.contains(positionDescription.packageName)) {
                showToastHandler("不建议添加此坐标，该应用为系统程序或白名单程序")
                return@setOnClickListener
            }

            mapPackagePositions[positionDescription.packageName] = PackagePositionDescription(positionDescription)
            dialogBinding.btnAddPosition.isEnabled = false
            dialogBinding.tvPackageName.text = "${positionDescription.packageName} (以下坐标数据已保存)"

            // 更新设置，记录用户选中的坐标
            settings.mapPackagePositions = mapPackagePositions
        }

        // 【获取窗口】，获取当前页面下所有空间的信息，并复制到手机的剪贴板中
        dialogBinding.btnDumpScreen.setOnClickListener {
            val root = service.rootInActiveWindow

            // 获取 root 下所有控件的信息
            val result = dumpRootNode(root)

            // 获取手机剪贴板
            val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("ACTIVITY", result)
            clipboard.setPrimaryClip(clipData)

            showToastHandler("该页面控件信息已复制到剪贴板！")
        }

        // 【退出】,关闭当前弹窗
        dialogBinding.btnQuit.setOnClickListener {
            dialogBinding.apply {
                btnShowOutline.text = "显示布局"
                btnAddWidget.isEnabled = false
                btnShowTarget.text = "显示准心"
                btnAddPosition.isEnabled = false
            }
            customizationDialogShowing = false
            windowManager.removeViewImmediate(customizationDialogView)
            windowManager.removeViewImmediate(frameOutlineLayout)
            windowManager.removeViewImmediate(imgPositionTarget)
        }
    }

    /**
     * 开启任务
     */
    private fun startSkipAdTask() {
        skipAdTaskRunning = true
        skipAdByActivityPosition = true
        skipAdByActivityWidget = true
        skipAdByKeyword = true
        setTargetedWidgets = null
        clickedWidgets.clear()

        // 移除当前的存在的移除事件，添加一个新的延时事件
        mainHandler.removeMessages(FastTouchService.ACTION_STOP_TASK)
        mainHandler.sendEmptyMessageDelayed(FastTouchService.ACTION_STOP_TASK, settings.skipAdDuration * 1000L)

    }

    /**
     * 结束任务
     */
    private fun stopSkipAdTask() {
        stopSkipAdTaskInner()
        mainHandler.removeMessages(FastTouchService.ACTION_STOP_TASK)
    }

    /**
     * 不再接受新的任务，但不影响当前已有的任务
     */
    private fun stopSkipAdTaskInner() {
        skipAdTaskRunning = false
        skipAdByActivityPosition = false
        skipAdByActivityWidget = false
        skipAdByKeyword = false
        setTargetedWidgets = null
    }

    /**
     * 提供给 Service 调用的打断任务的方法
     */
    fun onInterrupt() {
        stopSkipAdTask()
    }

    private fun showToastHandler(msg: String) {
        if (settings.showSkipAdToastFlag) {
            mainHandler.post{
                Toast.makeText(service, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}