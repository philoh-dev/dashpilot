package com.softwiredtech.dashpilot.datamodel.dash

/** Ordered selection; an absent list inherits the pin saved by older app versions. */
object PinnedControls {
    fun decode(encoded: String?, legacy: String?): List<String> =
        (encoded ?: legacy.orEmpty()).split(',').filter { it.isNotBlank() }.distinct()

    fun toggle(ids: List<String>, id: String): List<String> =
        if (id in ids) ids - id else ids + id
}
