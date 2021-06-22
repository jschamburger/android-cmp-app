package com.sourcepoint.cmplibrary.data.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

fun mockResponse(url: String, body: String, code: Int = 200, message: String = "OK"): Response {
    return Response.Builder()
        .code(code)
        .protocol(Protocol.HTTP_1_1)
        .request(Request.Builder().url(url).build())
        .message(message)
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()
}