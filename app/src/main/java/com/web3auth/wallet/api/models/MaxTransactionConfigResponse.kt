package com.web3auth.wallet.api.models

data class EthGasAPIResponse(
    val fast: Double,
    val fastest: Double,
    val safeLow: Double,
    val average: Double,
    val speed: Double,
    val safeLowWait: Double,
    val avgWait: Double,
    val fastestWait: Double,
    val fastWait: Double
)