package proxy

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * ProxyInterceptor - класс-перехватчик для обработки запросов и логирования
 */
class ProxyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        println("Intercepted request: ${originalRequest.url}")

        val modifiedRequest = originalRequest.newBuilder().url(originalRequest.url).build()

        return try {
            val response = chain.proceed(modifiedRequest)
            println("Received response with status code: ${response.code}")
            response
        } catch (e: IOException) {
            println("Error intercepting request: ${e.message}")
            Response.Builder()
                .request(modifiedRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Failed to process request")
                .body("Error occurred".toResponseBody(null))
                .build()
        }
    }
}