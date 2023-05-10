package com.webengage.inappsample

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.internal.ContextUtils.getActivity
import com.webengage.sdk.android.WebEngage
import com.webengage.sdk.android.actions.render.InAppNotificationData
import com.webengage.sdk.android.callbacks.InAppNotificationCallbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL


class MainActivity : AppCompatActivity(), InAppNotificationCallbacks {

    private val inAppsShown: HashSet<String> = HashSet()
    private val qualifiedInAppLists: HashMap<String, InAppNotificationData> = HashMap()
    private var inAppRenderInProgress: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onStart() {
        super.onStart()
        Log.d(Constants.TAG, "Main Activity")
        WebEngage.registerInAppNotificationCallback(this)
        WebEngage.get().analytics().screenNavigated("MAIN")
    }

    override fun onStop() {
        super.onStop()
        WebEngage.unregisterInAppNotificationCallback(this)
    }

    override fun onInAppNotificationPrepared(
        p0: Context?,
        p1: InAppNotificationData
    ): InAppNotificationData {
        Log.d(Constants.TAG, "onInAppNotificationPrepared ${p1.experimentId}")
        qualifiedInAppLists[p1.experimentId] = p1
        p1.setShouldRender(false)
        if (!inAppsShown.contains(p1.experimentId) && !inAppRenderInProgress) {
            CoroutineScope(Dispatchers.Main).launch{
                renderInApp(p1)
            }
        }
        return p1
    }

    private fun renderInApp(data: InAppNotificationData) {
        inAppRenderInProgress = true
        inAppsShown.add(data.experimentId)

        Log.d(Constants.TAG, "inApp data $data")

        trackImpression(data)

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        val ctaArray = data.data.getJSONArray("actions")
        if(ctaArray.length() > 0){
            val ctaText = ctaArray.getJSONObject(0).getString("actionText")
            var ctaDeeplink = ctaArray.getJSONObject(0).getString("actionLink")
            ctaDeeplink = ctaDeeplink.removePrefix("w://p/open_url_in_browser/")
            Log.d(Constants.TAG, " deeplink : ${Uri.decode(ctaDeeplink)}")

            val ctaActionId = ctaArray.getJSONObject(0).getString("actionEId")
            builder.setPositiveButton(ctaText,DialogInterface.OnClickListener{dialog, which ->
                trackClick(data, ctaActionId)
                Log.d(Constants.TAG, " perform onclick action here if required : ${Uri.decode(ctaDeeplink).toString()}")
            })
        }
        builder.setMessage(data.data["description"].toString())
            .setTitle("Custom Inapp ${data.experimentId}")
            .setNegativeButton("Close", DialogInterface.OnClickListener { dialog, which ->
                trackDismiss(data)
            })


        val dialog: AlertDialog = builder.create()

        dialog.show()
        Log.d(Constants.TAG, "rendering custom inApp ${data.experimentId}")
    }

    override fun onInAppNotificationShown(p0: Context?, p1: InAppNotificationData) {
        Log.d(Constants.TAG, "onInAppNotificationShown ${p1.experimentId}")
    }

    override fun onInAppNotificationClicked(
        p0: Context?,
        p1: InAppNotificationData,
        p2: String?
    ): Boolean {
        Log.d(Constants.TAG, "onInAppNotificationClicked ${p1.experimentId} : $p2")
        return false
    }

    override fun onInAppNotificationDismissed(p0: Context?, p1: InAppNotificationData) {
        inAppRenderInProgress = false
        qualifiedInAppLists.remove(p1.experimentId)
        Log.d(Constants.TAG, "onInAppNotificationDismissed ${p1.experimentId}")
    }

    fun trackImpression(inAppNotificationData: InAppNotificationData) {
        WebEngage.get().analytics()
            .trackSystem(Constants.NOTIFICATION_VIEW, getSystemMap(inAppNotificationData), null)
    }

    fun trackClick(inAppNotificationData: InAppNotificationData, ctaId: String) {
        val map = getSystemMap(inAppNotificationData)
        map[Constants.CTA_ID] = ctaId
        WebEngage.get().analytics()
            .trackSystem(Constants.NOTIFICATION_CLICK, map, null)

        inAppRenderInProgress = false

    }

    fun trackDismiss(inAppNotificationData: InAppNotificationData) {
        inAppRenderInProgress = false
        WebEngage.get().analytics()
            .trackSystem(Constants.NOTIFICATION_CLOSE, getSystemMap(inAppNotificationData), null)
    }

    fun getSystemMap(inAppNotificationData: InAppNotificationData): MutableMap<String, String> {
        val systemData: MutableMap<String, String> = mutableMapOf()
        systemData[Constants.EXPERIMENT_ID] = inAppNotificationData.experimentId
        systemData[Constants.NOTIFICATION_ID] = inAppNotificationData.variationId
        return systemData
    }
}