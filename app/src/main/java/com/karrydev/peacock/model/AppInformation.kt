package com.karrydev.peacock.model

import android.graphics.drawable.Drawable
import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination

data class AppInformation private constructor(
    val packageName: String,
    val appName: String,
    val appNamePinyin: String,
    val icon: Drawable,
    val checkFlag: Boolean = false
) : Comparable<AppInformation> {

    constructor(packageName: String, appName: String, icon: Drawable) : this(
        packageName,
        appName,
        try {
            PinyinHelper.toHanYuPinyinString(appName, HanyuPinyinOutputFormat(), "", true)
        } catch (e: BadHanyuPinyinOutputFormatCombination) {
            appName
        },
        icon,
    )

    override fun compareTo(other: AppInformation) =
        if (checkFlag && !other.checkFlag) {
            -11
        } else if (!checkFlag && other.checkFlag) {
            1
        } else {
            appNamePinyin.compareTo(other.appNamePinyin)
        }
}