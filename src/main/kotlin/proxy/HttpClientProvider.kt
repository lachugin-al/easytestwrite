package proxy

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * HttpClientProvider - Конфигурация OkHttp клиента с поддержкой HTTPS
 */
object HttpClientProvider {

    // Настраивает и возвращает OkHttp клиент с поддержкой SSL и перехватчиками
    val client: OkHttpClient by lazy {
        val trustManager = provideTrustManager()
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .addInterceptor(ProxyInterceptor())
            .build()
    }

    // Конфигурирует TrustManager для поддержки HTTPS соединений
    private fun provideTrustManager(): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        return trustManagerFactory.trustManagers[0] as X509TrustManager
    }
}