package com.sourcepoint.cmplibrary.creation

import android.app.Activity
import android.content.Context
import com.sourcepoint.cmplibrary.Campaign
import com.sourcepoint.cmplibrary.data.local.DataStorage
import com.sourcepoint.cmplibrary.data.local.create
import com.sourcepoint.cmplibrary.data.network.converter.JsonConverter
import com.sourcepoint.cmplibrary.data.network.converter.create
import com.sourcepoint.cmplibrary.data.network.util.ResponseManager
import com.sourcepoint.cmplibrary.data.network.util.create
import com.sourcepoint.cmplibrary.legislation.ccpa.CCPAConsentLib
import com.sourcepoint.cmplibrary.legislation.ccpa.CCPAConsentLibImpl
import com.sourcepoint.cmplibrary.legislation.gdpr.GDPRConsentLib
import com.sourcepoint.cmplibrary.legislation.gdpr.GDPRConsentLibImpl
import com.sourcepoint.cmplibrary.util.ConnectionManager
import com.sourcepoint.cmplibrary.util.ExecutorManager
import com.sourcepoint.cmplibrary.util.ViewsManager
import com.sourcepoint.cmplibrary.util.create
import com.sourcepoint.gdpr_cmplibrary.PrivacyManagerTab
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference

fun makeGdprConsentLib(
    accountId: Int,
    propertyName: String,
    propertyId: Int,
    pmId: String,
    context: Activity,
    privacyManagerTab: PrivacyManagerTab
): GDPRConsentLib {

    val account = Campaign(accountId, propertyId, propertyName, pmId)
    val appCtx: Context = context.applicationContext
    val client = createClientInfo()
    val errorManager = errorMessageManager(account, client)
    val logger = createLogger(errorManager)
    val jsonConverter = JsonConverter.create()
    val connManager = ConnectionManager.create(appCtx)
    val responseManager = ResponseManager.create(jsonConverter)
    val networkClient = networkClient(OkHttpClient(), responseManager)
    val dataStorage = DataStorage.create(appCtx)
    val viewManager = ViewsManager.create(WeakReference<Activity>(context))
    val execManager = ExecutorManager.create(appCtx)

    return GDPRConsentLibImpl(
        account, privacyManagerTab, appCtx, logger, jsonConverter, connManager, networkClient, dataStorage, viewManager, execManager
    )
}

fun makeCcpaConsentLib(
    accountId: Int,
    propertyName: String,
    propertyId: Int,
    pmId: String,
    context: Activity,
    privacyManagerTab: PrivacyManagerTab
): CCPAConsentLib {

    val account = Campaign(accountId, propertyId, propertyName, pmId)
    val appCtx: Context = context.applicationContext
    val client = createClientInfo()
    val errorManager = errorMessageManager(account, client)
    val logger = createLogger(errorManager)
    val jsonConverter = JsonConverter.create()
    val connManager = ConnectionManager.create(appCtx)
    val responseManager = ResponseManager.create(jsonConverter)
    val networkClient = networkClient(OkHttpClient(), responseManager)
    val dataStorage = DataStorage.create(appCtx)
    val viewManager = ViewsManager.create(WeakReference<Activity>(context))

    return CCPAConsentLibImpl()
}