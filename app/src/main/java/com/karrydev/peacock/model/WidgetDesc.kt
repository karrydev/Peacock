package com.karrydev.peacock.model

import android.graphics.Rect
import java.io.Serializable

data class WidgetDesc(
    val packageName: String = "",
    val activityName: String = "",
    val className: String = "",
    val idName: String = "",
    val text: String = "",
    val description: String = "",
    val rect: Rect = Rect(),
    val clickable: Boolean = false,
    val onlyClick: Boolean = false,
) : Serializable {
    constructor(widgetDesc: WidgetDesc) : this(
        widgetDesc.packageName,
        widgetDesc.activityName,
        widgetDesc.className,
        widgetDesc.idName,
        widgetDesc.text,
        widgetDesc.description,
        widgetDesc.rect,
        widgetDesc.clickable,
        widgetDesc.onlyClick
    )
}