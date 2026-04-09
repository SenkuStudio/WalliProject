package com.walli.wallpaper.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun String.asSafeFileName(): String =
    lowercase()
        .replace(Regex("[^a-z0-9-_]+"), "-")
        .replace(Regex("-+"), "-")
        .trim('-')
        .ifBlank { "wallpaper" }
