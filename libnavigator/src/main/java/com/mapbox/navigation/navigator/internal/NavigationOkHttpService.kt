package com.mapbox.navigation.navigator.internal

import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseCallback
import com.mapbox.common.HttpResponseData
import com.mapbox.common.HttpServiceInterface
import com.mapbox.navigation.navigator.BuildConfig
import java.io.IOException
import java.util.HashMap
import java.util.Locale
import java.util.UUID
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

internal class NavigationOkHttpService(
    private val logger: Logger? = null
) : HttpServiceInterface() {

    companion object {
        private const val HEADER_ENCODING = "Accept-Encoding"
        private const val GZIP = "gzip, deflate"
    }

    private val requestMap = hashMapOf<Long, Call>()

    private val httpClient: OkHttpClient by lazy {
        val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor(
                HttpLoggingInterceptor.Logger { message -> logger?.d(msg = Message(message)) }
            ).setLevel(HttpLoggingInterceptor.Level.BASIC)

            clientBuilder.addInterceptor(interceptor)
        }
        clientBuilder.build()
    }

    override fun setMaxRequestsPerHost(max: Byte) {
        httpClient.dispatcher().maxRequestsPerHost = max.toInt()
    }

    override fun supportsKeepCompression(): Boolean {
        return true
    }

    override fun request(request: HttpRequest, callback: HttpResponseCallback): Long {
        val requestBuilder = Request.Builder()
        val resourceUrl: String = request.url
        requestBuilder.url(resourceUrl).tag(resourceUrl.toLowerCase(Locale.US))
        for ((key, value) in request.headers) {
            requestBuilder.addHeader(key!!, value!!)
        }
        if (request.keepCompression) {
            // Adding this header manually means okhttp will not automatically decompress the data
            requestBuilder.addHeader(
                HEADER_ENCODING,
                GZIP
            )
        }
        val call = httpClient.newCall(requestBuilder.build())
        call.enqueue(
            HttpCallback(
                logger,
                request,
                callback
            )
        )

        var id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
        while (requestMap.containsKey(id)) {
            id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
        }
        requestMap[id] = call
        return id
    }

    override fun cancelRequest(id: Long) {
        requestMap.remove(id)?.cancel()
    }

    private class HttpCallback constructor(
        private val logger: Logger? = null,
        private val request: HttpRequest,
        private val callback: HttpResponseCallback
    ) : Callback {
        override fun onFailure(call: Call, e: IOException) {
            val result = ExpectedFactory.createError<HttpResponseData, HttpRequestError>(
                HttpRequestError(HttpRequestErrorType.OTHER_ERROR, e.message.toString())
            )
            runCallback(result)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val responseBody = response.body()
                if (responseBody == null) {
                    val result = ExpectedFactory.createError<HttpResponseData, HttpRequestError>(
                        HttpRequestError(HttpRequestErrorType.OTHER_ERROR, "empty body")
                    )
                    runCallback(result)
                    return
                }
                val responseData = HttpResponseData(
                    generateOutputHeaders(response),
                    response.code().toLong(),
                    responseBody.bytes()
                )
                val result = ExpectedFactory.createValue<HttpResponseData, HttpRequestError>(responseData)
                runCallback(result)
            } catch (exception: Exception) {
                val result = ExpectedFactory.createError<HttpResponseData, HttpRequestError>(
                    HttpRequestError(HttpRequestErrorType.OTHER_ERROR, exception.message.toString())
                )
                runCallback(result)
            }
        }

        private fun runCallback(result: Expected<HttpResponseData, HttpRequestError>) {
            if (result.isError) {
                logger?.e(msg = Message(result.error?.message.toString()))
            }
            callback.run(HttpResponse(request, result))
        }

        private fun generateOutputHeaders(response: Response): HashMap<String, String> {
            val outputHeaders = HashMap<String, String>()
            val responseHeaders = response.headers()
            for (i in 0 until responseHeaders.size()) {
                outputHeaders[responseHeaders.name(i)] = responseHeaders.value(i)
            }
            return outputHeaders
        }
    }
}
