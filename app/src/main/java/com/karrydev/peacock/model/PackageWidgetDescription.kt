package com.karrydev.peacock.model

import android.graphics.Rect
import java.io.Serializable

data class PackageWidgetDescription(
    var packageName: String = "",
    var activityName: String = "",
    var className: String = "",
    var idName: String = "",
    var text: String = "",
    var description: String = "",
    var rect: Rect = Rect(),
    var clickable: Boolean = false,
    var onlyClick: Boolean = false,
) : Serializable {
    constructor(packageWidgetDescription: PackageWidgetDescription) : this(
        packageWidgetDescription.packageName,
        packageWidgetDescription.activityName,
        packageWidgetDescription.className,
        packageWidgetDescription.idName,
        packageWidgetDescription.text,
        packageWidgetDescription.description,
        packageWidgetDescription.rect,
        packageWidgetDescription.clickable,
        packageWidgetDescription.onlyClick
    )
}