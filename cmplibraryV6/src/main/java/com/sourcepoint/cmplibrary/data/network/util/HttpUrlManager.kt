package com.sourcepoint.cmplibrary.data.network.util

import com.sourcepoint.cmplibrary.data.network.util.Env.PROD
import com.sourcepoint.cmplibrary.data.network.util.Env.STAGE
import com.sourcepoint.cmplibrary.exception.Legislation
import com.sourcepoint.cmplibrary.model.PMTab
import com.sourcepoint.cmplibrary.model.PmUrlConfig
import com.sourcepoint.cmplibrary.model.exposed.ActionType
import okhttp3.HttpUrl

/**
 * Component responsible of building and providing the URLs
 */
internal interface HttpUrlManager {
    fun inAppMessageUrl(env: Env): HttpUrl
    fun sendConsentUrl(actionType: ActionType, env: Env, legislation: Legislation): HttpUrl
    fun pmUrl(env: Env, legislation: Legislation, pmConfig: PmUrlConfig): HttpUrl
    fun ottUrlPm(pmConf: PmUrlConfig): HttpUrl
}

/**
 * Implementation of the [HttpUrlManager] interface
 */
internal object HttpUrlManagerSingleton : HttpUrlManager {

    private const val spHost = "cdn.privacy-mgmt.com"

    override fun inAppMessageUrl(env: Env): HttpUrl = when (env) {
        STAGE -> inAppUrlMessageStage
        PROD -> inAppUrlMessageProd
    }

    override fun sendConsentUrl(actionType: ActionType, env: Env, legislation: Legislation): HttpUrl {
        return when (legislation) {
            Legislation.CCPA -> {
                when (env) {
                    PROD -> HttpUrl.Builder()
                        .scheme("https")
                        .host("fake-wrapper-api.herokuapp.com")
                        .addPathSegments("all/v1/consent/$actionType")
                        .build() // sendCcpaConsentUrlStage(actionType = actionType.code)
                    STAGE -> sendCcpaConsentUrlProd(actionType = actionType.code)
                }
            }
            Legislation.GDPR -> {
                when (env) {
                    PROD -> HttpUrl.Builder()
                        .scheme("https")
                        .host("fake-wrapper-api.herokuapp.com")
                        .addPathSegments("all/v1/gdpr-consent")
                        .addQueryParameter("inApp", "true")
                        .addQueryParameter("env", "stage")
                        .build() // sendGdprConsentUrl
                    STAGE -> sendGdprConsentUrlProd(actionType = actionType.code)
                }
            }
        }
    }

    override fun pmUrl(env: Env, legislation: Legislation, pmConfig: PmUrlConfig): HttpUrl = when (legislation) {
        Legislation.GDPR -> urlPmGdpr(pmConfig)
        Legislation.CCPA -> urlPmCcpa() // urlUWPm(pmConfig!!, UrlLegislation.valueOf(legislation.name))
    }

    override fun ottUrlPm(pmConf: PmUrlConfig): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host(spHost)
        .addPathSegments("privacy-manager-ott")
        .addPathSegments("index.html")
        .addQueryParameter("consentLanguage", pmConf.consentLanguage)
        .addQueryParameter("consentUUID", pmConf.consentUUID)
        .apply {
            if (pmConf.pmTab != PMTab.DEFAULT) {
                addQueryParameter("pmTab", pmConf.pmTab.key)
            }
        }
        .addQueryParameter("site_id", pmConf.siteId)
        .addQueryParameter("message_id", pmConf.messageId)
        .build()

    val inAppUrlMessageStage: HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("cdn.sp-stage.net")
        .addPathSegments("wrapper/v2/messages")
        .addQueryParameter("env", "stage")
        .build()

    private val inAppUrlMessageProd: HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("cdn.privacy-mgmt.com")
        .addPathSegments("wrapper/v2/messages")
        .addQueryParameter("env", "localProd")
        .build()

    val inAppUrlNativeMessage: HttpUrl
        get() = HttpUrl.Builder()
            .scheme("https")
            .host(spHost)
            .addPathSegments("wrapper/tcfv2/v1/gdpr")
            .addPathSegments("native-message")
            .addQueryParameter("inApp", "true")
            .build()

    val sendLocalGdprConsentUrl: HttpUrl
        get() = HttpUrl.Builder()
            .scheme("http")
            .host("192.168.1.11")
            .port(3000)
            .addPathSegments("wrapper/tcfv2/v1/gdpr")
            .addPathSegments("consent")
            .addQueryParameter("env", "localProd")
            .addQueryParameter("inApp", "true")
            .addQueryParameter("sdkVersion", "AndroidLocal")
            .build()

    private fun urlPmGdpr(pmConf: PmUrlConfig): HttpUrl = HttpUrl.Builder()
        // https://notice.sp-stage.net/privacy-manager/index.html?message_id=<PM_ID>
        .scheme("https")
        .host("notice.sp-stage.net")
        .addPathSegments("privacy-manager/index.html")
        .addQueryParameter("pmTab", pmConf.pmTab.key)
        .apply {
            pmConf.consentLanguage?.let { addQueryParameter("consentLanguage", it) }
            pmConf.consentUUID?.let { addQueryParameter("consentUUID", it) }
            pmConf.siteId?.let { addQueryParameter("site_id", it) }
            pmConf.messageId?.let { addQueryParameter("message_id", it) }
        }
        .build()

    private fun urlPmCcpa(): HttpUrl = HttpUrl.parse("https://ccpa-inapp-pm.sp-prod.net?ccpa_origin=https://ccpa-service.sp-prod.net&privacy_manager_id=5df9105bcf42027ce707bb43&ccpaUUID=76c950be-45be-40ce-878b-c7bcf091722d&site_id=6099")!!

    private fun sendCcpaConsentUrlProd(actionType: Int): HttpUrl {
        // https://cdn.sp-stage.net/wrapper/v2/messages/ccpa/11?env=stage
        return HttpUrl.Builder()
            .scheme("https")
            .host("cdn.sp-stage.net")
            .addPathSegments("wrapper/v2/messages/ccpa/$actionType")
            .addQueryParameter("env", "stage")
            .build()
    }

    private fun sendGdprConsentUrlProd(actionType: Int): HttpUrl {
        // https://cdn.sp-stage.net/wrapper/v2/messages/gdpr/:actionType?env=stage
        return HttpUrl.Builder()
            .scheme("https")
            .host("cdn.sp-stage.net")
            .addPathSegments("wrapper/v2/messages/gdpr/$actionType")
            .addQueryParameter("env", "stage")
            .build()
    }
}

enum class UrlLegislation(val segment: String) {
    GDPR("segment_gdpr"),
    CCPA("segment_ccpa")
}

enum class Env {
    STAGE,
    PROD
}

enum class CampaignEnv(val value: String) {
    STAGE("stage"),
    PUBLIC("prod")
}
