package com.lonytech

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.*

class ApiClient private constructor() {

    private val client: OkHttpClient by lazy { getUnsafeOkHttpClient() }
    private val gson: Gson = GsonBuilder().setLenient().create()

    suspend fun post(
        endpoint: String,
        data: Map<String?, String?>,
        headers: Map<String?, String?>,
        context: Context?,
        ivString: String
    ): JSONObject = withContext(Dispatchers.IO) {
        if (!hasInternetConnection(context)) {
            return@withContext JSONObject().apply {
                put("success", false)
                put("message", "No Internet Connection. Try another Network")
                put("status", "false")
            }
        }

        val url = BASE_URL + endpoint
        val secretKey = API_KEY.take(32).toByteArray(Charsets.UTF_8)
        val iv = IvParameterSpec(ivString.toByteArray(Charsets.UTF_8))

        val stringPayload = JSONObject(data).toString()
        val encryptedData = encryptData(stringPayload, secretKey, iv)

        val endpointsToEncrypt = listOf(
            "payment/cardLess/debit",
            "payment/card/debit",
            "card-balance"
        )

        val payload = if (endpointsToEncrypt.contains(endpoint)) {
            mapOf("data" to encryptedData)
        } else {
            data
        }

        val jsonPayload = gson.toJson(payload)
        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)

        headers.forEach { (key, value) ->
            if (key != null && value != null) {
                requestBuilder.addHeader(key, value)
            }
        }

        Log.d("API", "URL: $url")
        Log.d("API", "Headers: $headers")
        Log.d("API", "Payload: $payload")

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: "{}"
                Log.d("API", "Response: $responseBody")
                Log.d("API", "Response code: ${response.code}")

                if (response.isSuccessful) {
                    JSONObject(responseBody)
                } else {
                    JSONObject().apply {
                        put("success", false)
                        put("message", if (response.code == 401) {
                            JSONObject(responseBody).optString("message", "Unauthorized")
                        } else {
                            "Unexpected error"
                        })
                        put("status", "false")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("API", "Error: ${e.message}")
            JSONObject().apply {
                put("success", false)
                put("message", e.message)
                put("status", "false")
            }
        }
    }

    private fun hasInternetConnection(context: Context?): Boolean {
        // Implementation for checking internet connection
        return true 
    }

    private fun encryptData(data: String, key: ByteArray, iv: IvParameterSpec): String {
        return try {
            val secretKeySpec = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            encrypted.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("API", "Encryption error: ${e.message}")
            ""
        }
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
            .build()
    }

    companion object {
        private const val BASE_URL = "https://abs.paylony.com/api/v1/"
        private const val API_KEY = "pos_hash_wv9b8dqovdpmz6r9v47gjtjy"
        val INSTANCE: ApiClient = ApiClient()
    }
}
