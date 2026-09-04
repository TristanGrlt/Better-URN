package org.better.urn.data

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object AndroidContextProvider {
    var context: Context? = null

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }
}
