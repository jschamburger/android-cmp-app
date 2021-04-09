package com.sourcepoint.cmplibrary.data.network.model

import com.sourcepoint.cmplibrary.core.Either
import com.sourcepoint.cmplibrary.core.map
import com.sourcepoint.cmplibrary.data.network.converter.failParam
import com.sourcepoint.cmplibrary.data.network.converter.toCCPAUserConsent
import com.sourcepoint.cmplibrary.exception.Legislation
import com.sourcepoint.cmplibrary.model.getFieldValue
import com.sourcepoint.cmplibrary.model.getMap
import com.sourcepoint.cmplibrary.model.toJSONObj
import com.sourcepoint.cmplibrary.model.toTreeMap
import com.sourcepoint.cmplibrary.util.check
import org.json.JSONObject

internal fun String.toUnifiedMessageRespDto1203(): UnifiedMessageResp1203 {
    return JSONObject(this).toUnifiedMessageRespDto1203()
}

internal fun JSONObject.toUnifiedMessageRespDto1203(): UnifiedMessageResp1203 {
    val map: Map<String, Any?> = this.toTreeMap()
    val localState = map.getFieldValue<String>("localState") ?: ""
    val propertyPriorityData = map.getMap("propertyPriorityData")?.toJSONObj() ?: failParam("propertyPriorityData")

    val listEither: List<Either<CampaignResp1203?>> = map
        .getFieldValue<List<Map<String, Any?>>>("campaigns")
        ?.map { check { it.toCampaignResp1203() } }
        ?: emptyList()

    val list = listEither.fold(mutableListOf<CampaignResp1203>()) { acc, elem ->
        elem.map { content -> content?.let { acc.add(content) } }
        acc
    }

    return UnifiedMessageResp1203(
        thisContent = this,
        campaigns = list,
        localState = localState,
        propertyPriorityData = propertyPriorityData
    )
}

internal fun Map<String, Any?>.toCampaignResp1203(): CampaignResp1203? {
    return when (getFieldValue<String>("type")?.toUpperCase() ?: failParam("type")) {
        Legislation.GDPR.name -> this.toGDPR1203()
        Legislation.CCPA.name -> this.toCCPA1203()
        else -> null
    }
}

internal fun String.toCCPA1203(): Ccpa1203? {
    val map: Map<String, Any?> = JSONObject(this).toTreeMap()
    return map.toCCPA1203()
}

private fun Map<String, Any?>.toCCPA1203(): Ccpa1203? {

    val message = getMap("message")?.toJSONObj()
    val messageMetaData = getMap("messageMetaData")?.toJSONObj()

    return Ccpa1203(
        thisContent = JSONObject(this),
        applies = getFieldValue<Boolean>("applies") ?: false,
        message = message,
        messageMetaData = messageMetaData,
        userConsent = getMap("userConsent")?.toCCPAUserConsent() ?: failParam("CCPAUserConsent")
    )
}

internal fun Map<String, Any?>.toGDPR1203(): Gdpr1203 {

    val message = getMap("message")?.toJSONObj()
    val messageMetaData = getMap("messageMetaData")?.toJSONObj()

    return Gdpr1203(
        thisContent = JSONObject(this),
        applies = getFieldValue<Boolean>("applies") ?: false,
        message = message,
        messageMetaData = messageMetaData,
        userConsent = getMap("userConsent")?.toGDPRUserConsent1203() ?: failParam("GDPRUserConsent")
    )
}

internal fun String.toGDPR1203(): Gdpr1203? {
    val map: Map<String, Any?> = JSONObject(this).toTreeMap()
    return map.toGDPR1203()
}

internal fun Map<String, Any?>.toGDPRUserConsent1203(): GDPRConsent1203 {

    val tcData: Map<String, Any?> = getMap("TCData") ?: emptyMap()
    val vendorsGrants = getMap("grants") ?: failParam("grants")
    val euConsent = getFieldValue<String>("euconsent") ?: failParam("euconsent")

    return GDPRConsent1203(
        thisContent = JSONObject(this),
        tcData = tcData,
        vendorsGrants = vendorsGrants,
        euConsent = euConsent
    )
}