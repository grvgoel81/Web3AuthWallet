package com.web3auth.wallet.api

import com.google.gson.GsonBuilder
import com.web3auth.core.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiHelper {

    private const val baseUrl = "https://api.tor.us"
    private const val ethUrl = "https://ethgasstation.info"
    private const val mockGasUrl = "https://gas-api.metaswap.codefi.network"

    private val okHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            if (BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.BODY
            }
        })
        .build()

    private val builder = GsonBuilder().disableHtmlEscaping().create()

    fun getTorusInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(builder))
            .client(okHttpClient)
            .build()
    }

    fun getEthInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(ethUrl)
            .addConverterFactory(GsonConverterFactory.create(builder))
            .client(okHttpClient)
            .build()
    }

    fun getMockGasInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(mockGasUrl)
            .addConverterFactory(GsonConverterFactory.create(builder))
            .client(okHttpClient)
            .build()
    }
}