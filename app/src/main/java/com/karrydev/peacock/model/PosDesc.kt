package com.karrydev.peacock.model

import java.io.Serializable

data class PosDesc (
    val packageName: String = "",
    val activityName: String = "",
    val x: Int = 0,
    val y: Int = 0
) : Serializable {
    constructor(desc: PosDesc) : this(
        desc.packageName,
        desc.activityName,
        desc.x,
        desc.y
    )
}