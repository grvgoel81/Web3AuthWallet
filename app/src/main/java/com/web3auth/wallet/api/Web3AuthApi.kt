package com.web3auth.wallet.api

import com.web3auth.wallet.api.models.EthGasAPIResponse
import com.web3auth.wallet.api.models.GasApiResponse
import com.web3auth.wallet.api.models.PriceResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface Web3AuthApi {
    @GET("/currency")
    suspend fun getCurrencyPrice(@Query("fsym") fsym: String, @Query("tsyms") tsyms: String): Response<PriceResponse>

    @GET("api/ethgasAPI.json")
    suspend fun getMaxTransactionConfig(): Response<EthGasAPIResponse>

    @GET(".")
    suspend fun getGasConfig(): Response<GasApiResponse>
}