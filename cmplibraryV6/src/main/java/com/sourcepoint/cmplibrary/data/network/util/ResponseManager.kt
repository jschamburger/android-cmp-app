package com.sourcepoint.cmplibrary.data.network.util

import com.sourcepoint.cmplibrary.core.Either
import com.sourcepoint.cmplibrary.exception.Legislation
import com.sourcepoint.cmplibrary.model.ConsentResp
import com.sourcepoint.cmplibrary.model.NativeMessageResp
import com.sourcepoint.cmplibrary.model.NativeMessageRespK
import com.sourcepoint.cmplibrary.model.UnifiedMessageResp1203
import okhttp3.Response

/**
 * Component used to parse an OkHttp [Response] and extract a DTO
 */
internal interface ResponseManager {

    /**
     * Parsing a [UnifiedMessageResp]
     * @param r http response
     * @return [Either] object
     */
    fun parseResponse1203(r: Response): Either<UnifiedMessageResp1203>

    /**
     * @param r http response
     * @return [Either] object
     */
    fun parseNativeMessRes(r: Response): Either<NativeMessageResp>

    fun parseNativeMessResK(r: Response): Either<NativeMessageRespK>

    fun parseConsentResEither(r: Response, legislation: Legislation): Either<ConsentResp>
    fun parseConsentRes(r: Response, legislation: Legislation): ConsentResp
    companion object
}
