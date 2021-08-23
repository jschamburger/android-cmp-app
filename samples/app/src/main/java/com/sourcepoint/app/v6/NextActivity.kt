package com.sourcepoint.app.v6

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.sourcepoint.cmplibrary.SpConsentLib
import com.sourcepoint.cmplibrary.UnitySpClient
import com.sourcepoint.cmplibrary.creation.SpConfigDataBuilder
import com.sourcepoint.cmplibrary.creation.delegate.spConsentLibLazy
import com.sourcepoint.cmplibrary.creation.makeConsentLib
import com.sourcepoint.cmplibrary.data.network.util.CampaignsEnv
import com.sourcepoint.cmplibrary.exception.CampaignType
import com.sourcepoint.cmplibrary.model.PMTab
import com.sourcepoint.cmplibrary.model.exposed.ActionType
import com.sourcepoint.cmplibrary.model.exposed.SPConsents
import org.json.JSONObject

class NextActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "**NextActivity"
    }

    private var spConsentLib: SpConsentLib? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)
        initSourcePoint()
        findViewById<View>(R.id.review_consents_gdpr1).setOnClickListener { _v: View? ->
            spConsentLib!!.loadPrivacyManager(
                    "509973",
                    PMTab.PURPOSES,
                    CampaignType.GDPR
            )
        }
    }

    override fun onResume() {
        super.onResume()
//        spConsentLib!!.loadMessage()      // if you uncomment this all will be fine
    }

    private fun initSourcePoint() {
        val cmpConfig = SpConfigDataBuilder()
                .addAccountId(229)
                .addPropertyName("wsj.android.app")
                .addMessageTimeout(4000)
                .addCampaignsEnv(CampaignsEnv.PUBLIC)
                .addCampaign(CampaignType.GDPR)
                .build()
        spConsentLib = makeConsentLib(
                cmpConfig,
                this,
                LocalClient()
        )
    }

        internal inner class LocalClient : UnitySpClient {
            override fun onMessageReady(message: JSONObject) {}
            override fun onError(error: Throwable) {
                error.printStackTrace()
            }

            override fun onConsentReady(consent: SPConsents) {
                Log.i(NextActivity.TAG, "onConsentReady: $consent")
            }

            override fun onConsentReady(consent: SPConsents, fromPm: Boolean) {
                Log.i(NextActivity.TAG, "onConsentReady: $consent FROM_PM? $fromPm")
            }

            override fun onConsentReady(consent: String) {
                Log.i(NextActivity.TAG, "onConsentReady: $consent")
            }

            override fun onUIFinished(view: View) {
                spConsentLib!!.removeView(view)
            }

            override fun onUIReady(view: View) {
                spConsentLib!!.showView(view)
            }

            override fun onAction(view: View, actionType: ActionType) {
                Log.i(NextActivity.TAG, "ActionType: $actionType")
            }
        }
}