package com.sourcepoint.cmplibrary

import android.content.Context
import android.view.View
import com.sourcepoint.cmplibrary.campaign.CampaignManager
import com.sourcepoint.cmplibrary.core.layout.NativeMessageClient
import com.sourcepoint.cmplibrary.core.layout.nat.NativeMessage
import com.sourcepoint.cmplibrary.core.layout.nat.NativeMessageInternal
import com.sourcepoint.cmplibrary.core.web.ConsentWebView
import com.sourcepoint.cmplibrary.core.web.JSClientLib
import com.sourcepoint.cmplibrary.data.Service
import com.sourcepoint.cmplibrary.data.local.DataStorage
import com.sourcepoint.cmplibrary.data.network.converter.JsonConverter
import com.sourcepoint.cmplibrary.data.network.converter.fail
import com.sourcepoint.cmplibrary.data.network.model.ConsentAction
import com.sourcepoint.cmplibrary.data.network.util.HttpUrlManager
import com.sourcepoint.cmplibrary.data.network.util.HttpUrlManagerSingleton
import com.sourcepoint.cmplibrary.exception.GenericSDKException
import com.sourcepoint.cmplibrary.exception.Logger
import com.sourcepoint.cmplibrary.exception.MissingClientException
import com.sourcepoint.cmplibrary.exception.RenderingAppException
import com.sourcepoint.cmplibrary.model.ActionType
import com.sourcepoint.cmplibrary.model.Campaign
import com.sourcepoint.cmplibrary.model.PrivacyManagerTabK
import com.sourcepoint.cmplibrary.model.toMessageReqMock
import com.sourcepoint.cmplibrary.util.* // ktlint-disable

internal class SpConsentLibImpl(
    internal val campaign: Campaign,
    internal val pPrivacyManagerTab: PrivacyManagerTabK,
    internal val context: Context,
    internal val pLogger: Logger,
    internal val pJsonConverter: JsonConverter,
    internal val service: Service,
    internal val executor: ExecutorManager,
    private val pConnectionManager: ConnectionManager,
    private val viewManager: ViewsManager,
    private val dataStorage: DataStorage,
    private val campaignManager: CampaignManager,
    private val urlManager: HttpUrlManager = HttpUrlManagerSingleton
) : SpConsentLib {

    override var spClient: SpClient? = null
    private val nativeMsgClient by lazy { NativeMsgDelegate() }

    /** Start Client's methods */
    override fun loadMessage(authId: String) {
        checkMainThread("loadMessage")
        throwsExceptionIfClientNoSet()
        service.getMessage(
            messageReq = campaign.toMessageReqMock(),
            pSuccess = { messageResp -> },
            pError = { throwable -> }
        )
    }

    override fun loadMessage() {
        checkMainThread("loadMessage")
        throwsExceptionIfClientNoSet()

        if (viewManager.isViewInLayout) return

        service.getMessage(
            messageReq = campaign.toMessageReqMock(),
            pSuccess = { messageResp ->
                executor.executeOnMain {
                    val webView = viewManager.createWebView(this, JSReceiverDelegate())
                    (webView as? ConsentWebView)?.let {
                        // TODO we have to choose which one to show, GDPR or CCPA?
                        val mess = messageResp.campaigns.first().message
//                        val mess = messageResp.campaigns.last().message
                        it.loadConsentUIFromUrl(urlManager.urlURenderingAppStage(), mess!!)
                    } ?: throw RuntimeException("webView is not a ConsentWebView")
                }
            },
            pError = { throwable ->
                spClient?.onError(throwable.toConsentLibException())
            }
        )
    }

    override fun loadMessage(nativeMessage: NativeMessage) {
        checkMainThread("loadMessage")
        throwsExceptionIfClientNoSet()

        service.getNativeMessageK(
            campaign.toMessageReqMock(),
            { messageResp ->
                executor.executeOnMain {
                    /** configuring onClickListener and set the parameters */
                    (nativeMessage as? NativeMessageInternal)?.setAttributes(messageResp.msg)
                    /** set the action callback */
                    (nativeMessage as? NativeMessageInternal)?.setActionClient(nativeMsgClient)
                    /** calling the client */
                    spClient?.onUIReady(nativeMessage)
                }
            },
            { throwable -> pLogger.error(throwable.toConsentLibException()) }
        )
    }

    override fun loadGDPRPrivacyManager() {
        checkMainThread("loadPrivacyManager")
        throwsExceptionIfClientNoSet()
        val pmConfig = campaignManager.getPmGDPRConfig()
        pmConfig
            .map {
                val webView = viewManager.createWebView(this, JSReceiverDelegate())
                webView?.loadConsentUIFromUrl(urlManager.urlPm(it))
            }
            .executeOnLeft { fail("GDPR Privacy Manager config is missing!!") }
    }

    override fun loadCCPAPrivacyManager() {
        checkMainThread("loadPrivacyManager")
        throwsExceptionIfClientNoSet()
    }

    override fun showView(view: View) {
        checkMainThread("showView")
        viewManager.showView(view)
    }

    override fun removeView(view: View) {
        checkMainThread("removeView")
        viewManager.removeView(view)
    }

    override fun dispose() {
        executor.dispose()
        viewManager.removeAllViews()
    }

    //    /** Start Receiver methods */
    inner class JSReceiverDelegate : JSClientLib {
        //
        override fun onConsentUIReady(view: View, isFromPM: Boolean) {
            // TODO what consent is ready? GDPR or CCPA?
            view.let { viewManager.showView(it) }
        }

        override fun log(view: View, tag: String?, msg: String?) {
            pLogger.i("ConsentLibImpl", "js =================== log")
        }

        override fun log(view: View, msg: String?) {
            pLogger.i("ConsentLibImpl", "js =================== log")
        }

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
            spClient?.onError(error)
        }

        override fun onAction(view: View, actionData: String) {
            /** spClient is called from [onActionFromWebViewClient] */
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
    internal fun onActionFromWebViewClient(action: ConsentAction, view: View) {
        executor.executeOnMain {
            spClient?.onAction(view, action.actionType)
            when (action.actionType) {
                ActionType.ACCEPT_ALL -> {
                    view.let { spClient?.onUIFinished(it) }
                }
                ActionType.MSG_CANCEL -> {
                    view.let { spClient?.onUIFinished(it) }
                }
                ActionType.SAVE_AND_EXIT -> {
                    view.let { spClient?.onUIFinished(it) }
                }
                ActionType.SHOW_OPTIONS -> {
                    view.let { spClient?.onUIFinished(it) }
                }
                ActionType.REJECT_ALL -> {
                    view.let { spClient?.onUIFinished(it) }
                }
                ActionType.PM_DISMISS -> {
                    view.let { spClient?.onUIFinished(it) }
                }
            }
        }
    }

    /**
     * Delegate used by the [NativeMessage] to catch events performed by the user
     */
    inner class NativeMsgDelegate : NativeMessageClient {

        override fun onClickAcceptAll(view: View, ca: ConsentAction) {
            spClient?.onAction(view, ActionType.ACCEPT_ALL)
        }

        override fun onClickRejectAll(view: View, ca: ConsentAction) {
            spClient?.onAction(view, ActionType.REJECT_ALL)
        }

        override fun onPmDismiss(view: View, ca: ConsentAction) {}

        override fun onClickShowOptions(view: View, ca: ConsentAction) {
            spClient?.onAction(view, ActionType.SHOW_OPTIONS)
        }

        override fun onClickCancel(view: View, ca: ConsentAction) {
            spClient?.onAction(view, ActionType.MSG_CANCEL)
        }

        override fun onDefaultAction(view: View, ca: ConsentAction) {
        }
    }
}
