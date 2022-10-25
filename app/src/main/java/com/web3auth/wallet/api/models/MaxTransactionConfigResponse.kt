package com.web3auth.wallet.api.models

data class MaxTransactionConfigResponse(
    val low: Params? = null,
    val medium: Params? = null,
    val high: Params? = null
)

data class Params(
    val minWaitTimeEstimate: Double? = null,
    val maxWaitTimeEstimate: Double? = null,
    val suggestedMaxPriorityFeePerGas: String? = null,
    val suggestedMaxFeePerGas: String? = null
)