package com.sourcepoint.cmplibrary.data

import com.sourcepoint.cmplibrary.data.local.DataStorage
import com.sourcepoint.cmplibrary.data.network.NetworkClient
import com.sourcepoint.cmplibrary.data.network.model.MessageReq
import com.sourcepoint.cmplibrary.data.network.model.MessageResp
import com.sourcepoint.cmplibrary.data.network.model.NativeMessageResp
import com.sourcepoint.cmplibrary.data.network.model.getAppliedLegislation
import com.sourcepoint.cmplibrary.util.executeOnLeft
import com.sourcepoint.cmplibrary.util.map

/**
 * Factory method to create an instance of a [Service] using its implementation
 * @param nc is an instance of [NetworkClient]
 * @param ds is an instance of [DataStorage]
 * @return an instance of the [ServiceImpl] implementation
 */
internal fun Service.Companion.create(nc: NetworkClient, ds: DataStorage): Service = ServiceImpl(nc, ds)

/**
 * Implementation os the [Service] interface
 */
private class ServiceImpl(
    private val nc: NetworkClient,
    private val ds: DataStorage
) : Service, NetworkClient by nc, DataStorage by ds {

    override fun getMessage(messageReq: MessageReq, pSuccess: (MessageResp) -> Unit, pError: (Throwable) -> Unit) {
        nc.getMessage(
            messageReq,
            { messageResp ->
                messageResp
                    .getAppliedLegislation()
                    .map {
                        pSuccess(messageResp)
                        saveAppliedLegislation(it.name)
                    }
                    .executeOnLeft { pError(it) }
            },
            pError
        )
    }

    override fun getNativeMessage(messageReq: MessageReq, success: (NativeMessageResp) -> Unit, error: (Throwable) -> Unit) {
        nc.getNativeMessage(
            messageReq,
            { nativeMessageResp ->
                success(nativeMessageResp)
                // TODO save the data into the local storage
            },
            ::error
        )
    }
}
