package com.manonsunderground.model

data class MapFrequency(
    val mapname: String?,
    val count: Long,
    val mapFullName: String? = null
) {
    constructor(mapname: String?, count: Long) : this(mapname, count, null)
}
