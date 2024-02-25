package com.karrydev.peacock.model

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.karrydev.peacock.util.appContext
import java.util.TreeMap

/**
 * 用户设置类
 * 相关设置会以 SharedPreferences 进行本地存储
 */
object Setting {

    private const val SP_NAME = "Peacock_NEW_Config"

    private lateinit var sp: SharedPreferences
    private lateinit var editor: Editor
    private lateinit var gson: Gson

    /**
     *  跳过广告时是否显示通知
     */
    private const val SHOW_SKIP_AD_TOAST_FLAG = "show_skip_ad_toast_flag"
    var showSkipAdToastFlag = false
        set(value) {
            if (value != field) {
                editor.putBoolean(SHOW_SKIP_AD_TOAST_FLAG, value)
                editor.apply()
                field = value
            }
        }

    /**
     *  展示前台通知
     */
    private const val SHOW_FOREGROUND_NOTIFICATION_FLAG = "show_foreground_notification_flag"
    var showForegroundNotification = false
        set(value) {
            if (value != field) {
                editor.putBoolean(SHOW_FOREGROUND_NOTIFICATION_FLAG, value)
                editor.apply()
                field = value
            }
        }

    /**
     * 检测时长
     */
    private const val SKIP_AD_DURATION = "SKIP_AD_DURATION"
    var skipAdDuration = 4
        set(value) {
            if (value != field) {
                field = value
                editor.putInt(SKIP_AD_DURATION, field)
                editor.apply()
            }
        }


    /**
     * 关键词列表
     */
    private const val KEYWORDS_LIST = "KEYWORDS_LIST"
    var keywordList = mutableListOf<String>()
        set(value) {
            field.clear()
            field.addAll(value)
            editor.putString(KEYWORDS_LIST, gson.toJson(keywordList))
            editor.apply()
        }

    /**
     * 应用白名单列表
     */
    private const val WHITELIST_PACKAGE = "WHITELIST_PACKAGE"
    var whiteListSet = HashSet<String>()
        set(value) {
            field.clear()
            field.addAll(value)
            // 这里必须要创建一个新的 Set 对象放进去，原文如下：
            // https://developer.android.com/reference/android/content/SharedPreferences.html
            // Note that you must not modify the set instance returned by this call.
            // The consistency of the stored data is not guaranteed if you do, nor is your ability to modify the instance at all.
            editor.putStringSet(WHITELIST_PACKAGE, HashSet(field))
            editor.apply()
        }

    /**
     * 用户添加的控件集合
     */
    private const val PACKAGE_WIDGETS_LIST = "PACKAGE_WIDGETS_LIST"
    var pkgWidgetMap = TreeMap<String, Set<PackageWidgetDescription>>()
        set(value) {
            field = value
            editor.putString(PACKAGE_WIDGETS_LIST, gson.toJson(field))
            editor.apply()
        }
    fun setPackageWidgetsInString(value: String): Boolean {
        return try {
            pkgWidgetMap = gson.fromJson(value, TreeMap<String, Set<PackageWidgetDescription>>().javaClass)
            true
        } catch (e: JsonSyntaxException) {
            false
        }
    }

    /**
     * 用户添加的坐标集合
     */
    private const val PACKAGE_POSITIONS_LIST = "PACKAGE_POSITIONS_LIST"
    var pkgPosMap = TreeMap<String, PackagePositionDescription>()
        set(value) {
            field = value
            editor.putString(PACKAGE_POSITIONS_LIST, gson.toJson(field))
            editor.apply()
        }

    init {
        initSettings()
    }

    private fun initSettings() {
        sp = appContext.getSharedPreferences(SP_NAME, Activity.MODE_PRIVATE)
        editor = sp.edit()
        gson = Gson()

        // 从 SharedPreferences 获取所有设置
        showSkipAdToastFlag = sp.getBoolean(SHOW_SKIP_AD_TOAST_FLAG, true)
        skipAdDuration = sp.getInt(SKIP_AD_DURATION, 4)

        // 初始化关键字列表
        // 这里谨慎设置「关闭」，因为很多正常弹窗的关闭icon的desc都是「关闭」，容易导致误关
        var json = sp.getString(KEYWORDS_LIST, "[\"跳过\"]")
        if (json != null) {
            keywordList.addAll(gson.fromJson(json, ArrayList<String>().javaClass))
        }

        // 查找所有系统包，系统包默认放入白名单里
        val packageManager = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val sysPkgSet = HashSet<String>()
        resolveInfoList.forEach { info ->
            if ((info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
                // 如果是系统包则记录
                sysPkgSet.add(info.activityInfo.packageName)
            }
        }
        // 把这个包也放入白名单
        sp.getStringSet(WHITELIST_PACKAGE, sysPkgSet)?.let { whiteListSet.addAll(it) }

        // 加载用户添加的控件
        json = sp.getString(PACKAGE_WIDGETS_LIST, null)
        if (json != null) {
            pkgWidgetMap.putAll(gson.fromJson(json, TreeMap<String, Set<PackageWidgetDescription>>().javaClass))
        }

        // 加载用户添加的坐标
        json = sp.getString(PACKAGE_POSITIONS_LIST, null)
        if (json != null) {
            pkgPosMap.putAll(gson.fromJson(json, TreeMap<String, PackagePositionDescription>().javaClass))
        }
    }
}