package com.web3auth.wallet.api

import com.web3auth.wallet.api.models.MaxTransactionConfigResponse
import com.web3auth.wallet.api.models.PriceResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface Web3AuthApi {
    @GET("/currency")
    suspend fun getCurrencyPrice(@Query("fsym") fsym: String, @Query("tsyms") tsyms: String): Response<PriceResponse>

    @GET
    suspend fun getMaxTransactionConfig(): Response<MaxTransactionConfigResponse>
}