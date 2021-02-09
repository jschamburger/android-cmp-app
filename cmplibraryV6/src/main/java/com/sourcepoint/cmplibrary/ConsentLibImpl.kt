package com.sourcepoint.cmplibrary

import android.content.Context
import android.view.View
import com.sourcepoint.cmplibrary.core.NativeMessage
import com.sourcepoint.cmplibrary.core.NativeMessageClient
import com.sourcepoint.cmplibrary.core.web.ConsentWebView
import com.sourcepoint.cmplibrary.core.web.JSClientLib
import com.sourcepoint.cmplibrary.data.Service
import com.sourcepoint.cmplibrary.data.network.converter.JsonConverter
import com.sourcepoint.cmplibrary.data.network.model.ConsentAction
import com.sourcepoint.cmplibrary.data.network.model.PmUrlConfig
import com.sourcepoint.cmplibrary.data.network.util.HttpUrlManager
import com.sourcepoint.cmplibrary.data.network.util.HttpUrlManagerSingleton
import com.sourcepoint.cmplibrary.exception.GenericSDKException
import com.sourcepoint.cmplibrary.exception.Logger
import com.sourcepoint.cmplibrary.exception.MissingClientException
import com.sourcepoint.cmplibrary.exception.RenderingAppException
import com.sourcepoint.cmplibrary.model.ActionType
import com.sourcepoint.cmplibrary.model.Campaign
import com.sourcepoint.cmplibrary.model.PrivacyManagerTabK
import com.sourcepoint.cmplibrary.model.toMessageReq
import com.sourcepoint.cmplibrary.util.* //ktlint-disable

internal class ConsentLibImpl(
    private val urlManager: HttpUrlManager = HttpUrlManagerSingleton,
    internal val campaign: Campaign,
    internal val pPrivacyManagerTab: PrivacyManagerTabK,
    internal val context: Context,
    internal val pLogger: Logger,
    internal val pJsonConverter: JsonConverter,
    private val pConnectionManager: ConnectionManager,
    internal val service: Service,
    private val viewManager: ViewsManager,
    private val executor: ExecutorManager
) : ConsentLib {

    override var spClient: SpClient? = null
    private val nativeMsgClient by lazy { NativeMsgDelegate() }

    /** Start Client's methods */
    override fun loadMessage(authId: String) {
        checkMainThread("loadMessage")
        throwsExceptionIfClientNoSet()
        service.getMessage(
            messageReq = campaign.toMessageReq(),
            pSuccess = { messageResp -> },
            pError = { throwable -> }
        )
    }

    override fun loadMessage() {
        checkMainThread("loadMessage")
        throwsExceptionIfClientNoSet()

        service.getMessage(
            messageReq = campaign.toMessageReq(),
            pSuccess = { messageResp ->
                executor.executeOnMain {
                    val webView = viewManager.createWebView(this, JSReceiverDelegate())
                    (webView as? ConsentWebView)?.let {
                        it.settings
                        it.loadConsentUIFromUrl(urlManager.urlLocalTest(), messageResp.message)
                    } ?: throw RuntimeException("webView is not a ConsentWebView")
                }
            },
            pError = { throwable -> spClient?.onError(throwable.toConsentLibException()) }
        )
    }

    override fun loadMessage(nativeMessage: NativeMessage) {
        checkMainThread("loadMessage")
        throwsExceptionIfClientNoSet()

        service.getNativeMessage(
            campaign.toMessageReq(),
            { messageResp ->
                val jsonResult = messageResp.msgJSON
                executor.executeOnMain {
                    /** configuring onClickListener and set the parameters */
//                    nativeMessage.setAttributes(NativeMessageAttrs(jsonResult, pLogger))
                    /** set the action callback */
//                    nativeMessage.setActionClient(nativeMsgClient)
                    /** calling the client */
                    spClient?.onConsentUIReady(nativeMessage)
                }
            },
            { throwable -> pLogger.error(throwable.toConsentLibException()) }
        )
    }

    override fun loadMessage(authId: String, nativeMessage: NativeMessage) {
        checkMainThread("loadMessage")
        throwsExceptionIfClientNoSet()
    }

    override fun loadPrivacyManager() {
        checkMainThread("loadPrivacyManager")
        throwsExceptionIfClientNoSet()
        val webView = viewManager.createWebView(this, JSReceiverDelegate())
        val pmConfig = PmUrlConfig(
            consentUUID = "89b2d14b-70ee-4344-8cc2-1b7b281d0f2d",
            siteId = "7639",
            messageId = campaign.pmId
        )
//        webView?.loadConsentUIFromUrl(urlManager.urlPm(pmConfig), messageResp.gdpr!!.message)
    }

    override fun loadPrivacyManager(authId: String) {
        checkMainThread("loadPrivacyManager")
        throwsExceptionIfClientNoSet()
    }

    override fun showView(view: View) {
        checkMainThread("showView")
        viewManager.showView(view)
    }

    override fun removeView(view: View?) {
        checkMainThread("removeView")
        viewManager.removeView(view)
    }

    override fun dispose() {
        executor.dispose()
    }

    //    /** Start Receiver methods */
    inner class JSReceiverDelegate : JSClientLib {
        //
        override fun onConsentUIReady(wv: View, isFromPM: Boolean) {
            pLogger.i("ConsentLibImpl", "js ===================== msg [onConsentUIReady]  ===========================")
            wv.let { viewManager.showView(it) } ?: throw GenericSDKException(description = "WebView is null")
        }

        override fun log(wv: View, tag: String?, msg: String?) {
            pLogger.i("ConsentLibImpl", "js =================== log")
        }

        override fun log(wv: View, msg: String?) {
            pLogger.i("ConsentLibImpl", "js =================== log")
        }

//        override fun onAction(actionData: String) {
//            pLogger.i("ConsentLibImpl", "js ===================== msg actionData [$actionData]  ===========================")
//            pJsonConverter
//                .toConsentAction(actionData)
//                .map { onActionFromWebViewClient(it) }
//                .executeOnLeft { throw it }
//        }

        override fun onError(view: View, errorMessage: String) {
            pLogger.i("ConsentLibImpl", "js ===================== msg errorMessage [$errorMessage]  ===========================")
            spClient?.onError(GenericSDKException(description = errorMessage))
            pLogger.error(RenderingAppException(description = errorMessage, pCode = errorMessage))
        }

        override fun onNoIntentActivitiesFoundFor(view: View, url: String) {
            pLogger.i("ConsentLibImpl", "js ===================== msg url [$url]  ===========================")
        }

        override fun onError(view: View, error: Throwable) {
            pLogger.i("ConsentLibImpl", "js ===================== msg onError [$error]  ===========================")
            throw error
        }

        override fun onAction(view: View, actionData: String) {
            pJsonConverter
                .toConsentAction(actionData)
                .map { onActionFromWebViewClient(it, view) }
                .executeOnLeft { throw it }
        }
    }

    /** End Receiver methods */

    private fun throwsExceptionIfClientNoSet() {
        spClient ?: throw MissingClientException(description = "spClient instance is missing")
    }

    /**
     * Receive the action performed by the user from the WebView
     */
    internal fun onActionFromWebViewClient(action: ConsentAction, view: View? = null) {
        executor.executeOnMain { spClient?.onAction(action.actionType) }
        when (action.actionType) {
            ActionType.ACCEPT_ALL -> {
            }
            ActionType.MSG_CANCEL -> {
                view?.let {
                    viewManager.removeView(view)
                }
            }
            ActionType.SAVE_AND_EXIT -> {
            }
            ActionType.SHOW_OPTIONS -> {
            }
            ActionType.REJECT_ALL -> {
            }
            ActionType.PM_DISMISS -> {
            }
        }
    }

    /**
     * Delegate used by the [NativeMessage] to catch events performed by the user
     */
    inner class NativeMsgDelegate : NativeMessageClient {
        /**
         * onclick listener connected to the acceptAll button in the NativeMessage View
         */
        override fun onClickAcceptAll(ca: ConsentAction) {
            spClient?.onAction(ActionType.ACCEPT_ALL)
        }

        /**
         * onclick listener connected to the RejectAll button in the NativeMessage View
         */
        override fun onClickRejectAll(ca: ConsentAction) {
            spClient?.onAction(ActionType.REJECT_ALL)
        }

        override fun onPmDismiss(ca: ConsentAction) {}

        /**
         * onclick listener connected to the ShowOptions button in the NativeMessage View
         */
        override fun onClickShowOptions(ca: ConsentAction) {
            spClient?.onAction(ActionType.SHOW_OPTIONS)
        }

        /**
         * onclick listener connected to the Cancel button in the NativeMessage View
         */
        override fun onClickCancel(ca: ConsentAction) {
            spClient?.onAction(ActionType.MSG_CANCEL)
        }

        override fun onDefaultAction(ca: ConsentAction) {
        }
    }
}
