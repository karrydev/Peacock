package com.karrydev.fasttouch.model

import java.io.Serializable

data class PackagePositionDescription (
    var packageName: String = "",
    var activityName: String = "",
    var x: Int = 0,
    var y: Int = 0
) : Serializable {
    constructor(desc: PackagePositionDescription) : this(
        desc.packageName,
        desc.activityName,
        desc.x,
        desc.y
    )
}