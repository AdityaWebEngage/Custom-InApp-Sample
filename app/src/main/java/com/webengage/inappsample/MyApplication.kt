package com.webengage.inappsample

import android.app.Application
import android.content.Context
import android.util.Log
import com.webengage.sdk.android.WebEngageConfig
import com.webengage.sdk.android.WebEngageActivityLifeCycleCallbacks
import com.webengage.sdk.android.actions.render.InAppNotificationData
import com.webengage.sdk.android.callbacks.InAppNotificationCallbacks


class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val config = WebEngageConfig.Builder().setWebEngageKey(Constants.LICENSE_CODE)
            .setDebugMode(true)
            .build()
        this.registerActivityLifecycleCallbacks(WebEngageActivityLifeCycleCallbacks(this,config))

    }

}