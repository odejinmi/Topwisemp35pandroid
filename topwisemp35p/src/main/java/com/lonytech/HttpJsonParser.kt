package com.lonytech

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.*
import kotlin.text.Charsets

/**
 * A utility class to perform HTTP requests and parse JSON responses.
 * Note: Network operations should be performed on a background thread.
 * 
 * Refactored to use OkHttp for better reliability and to fix SocketException: EPERM issues.
 */
class HttpJsonParser(private val context: Context? = null) {
    private val BASE_URL = "https://abs.paylony.com/api/v1/"
    private val API_KEY = "pos_hash_wv9b8dqovdpmz6r9v47gjtjy"

    companion object {
        private val client: OkHttpClient by lazy {
            getUnsafeOkHttpClient()
        }

        private fun getUnsafeOkHttpClient(): OkHttpClient {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            
            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                .build()
        }
    }

    /**
     * Makes an HTTP request to the specified endpoint.
     *
     * @param endpoint The API endpoint (e.g., "payment/card/debit")
     * @param method The HTTP method (e.g., "GET", "POST", "PATCH")
     * @param params The request parameters
     * @param headers The request headers
     * @param ivString The initialization vector string for encryption
     * @return A JSONObject containing the response, or null if an error occurred.
     */
    fun makeHttpRequest(
        endpoint: String,
        method: String,
        params: Map<String?, String?>?,
        headers: Map<String?, String?>,
        ivString: String
    ): JSONObject? {
        try {
            // Check network connectivity first
            if (!isNetworkAvailable()) {
                Log.e("HttpJsonParser", "No network connectivity available")
                return null
            }
            
            val url = BASE_URL + endpoint
            val secretKey = API_KEY.take(32).toByteArray(Charsets.UTF_8)
            val iv = IvParameterSpec(ivString.toByteArray(Charsets.UTF_8))
            
            val encryptedData = if (params != null) {
                val jsonParams = JSONObject(params).toString()
                encryptData(jsonParams, secretKey, iv)
            } else null

            val endpointsToEncrypt = listOf(
                "payment/cardLess/debit",
                "payment/card/debit",
                "card-balance"
            )

            // Determine if we should send encrypted data or the original params
            val requestData = if (endpointsToEncrypt.contains(endpoint) && encryptedData != null) {
                mapOf("data" to encryptedData)
            } else {
                params
            }

            val actualMethod = method.replace("&TOKEN", "")
            val requestBuilder = Request.Builder()
            
            // Set Headers
            headers.forEach { (key, value) ->
                if (key != null && value != null) {
                    requestBuilder.addHeader(key, value)
                }
            }

            val request = if (actualMethod == "GET") {
                val httpUrlBuilder = url.toHttpUrl().newBuilder()
                requestData?.forEach { (key, value) ->
                    if (key != null && value != null) {
                        httpUrlBuilder.addQueryParameter(key, value)
                    }
                }
                requestBuilder.url(httpUrlBuilder.build()).get().build()
            } else {
                // For POST/PATCH, we use FormBody (application/x-www-form-urlencoded)
                val formBodyBuilder = FormBody.Builder()
                requestData?.forEach { (key, value) ->
                    if (key != null && value != null) {
                        formBodyBuilder.add(key, value)
                    }
                }
                val requestBody = formBodyBuilder.build()
                requestBuilder.url(url).method(actualMethod, requestBody).build()
            }

            Log.d("HttpJsonParser", "Request URL: ${request.url}")
            Log.d("HttpJsonParser", "Request Method: ${request.method}")

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                Log.d("HttpJsonParser", "Response Code: ${response.code}")
                
                if (responseBody.isNotEmpty()) {
                    return try {
                        JSONObject(responseBody)
                    } catch (e: Exception) {
                        Log.e("HttpJsonParser", "Error parsing JSON response: ${e.message}\nRaw response: $responseBody")
                        null
                    }
                } else {
                    Log.w("HttpJsonParser", "Empty response received")
                }
            }
        } catch (e: Exception) {
            Log.e("HttpJsonParser", "Error in makeHttpRequest: ${e.message}", e)
        }
        return null
    }

    private fun encryptData(data: String, key: ByteArray, iv: IvParameterSpec): String {
        return try {
            val secretKeySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            // Convert byte array to hex string
            encrypted.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("HttpJsonParser", "Encryption error: ${e.message}")
            ""
        }
    }

    /**
     * Check if network connectivity is available
     */
    private fun isNetworkAvailable(): Boolean {
        if (context == null) {
            Log.w("HttpJsonParser", "Context not provided, skipping network check")
            return true // Assume network is available if context is not provided
        }
        
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e("HttpJsonParser", "Error checking network connectivity: ${e.message}")
            true // Assume network is available if check fails
        }
    }
}
