package com.web3auth.wallet.utils

import org.web3j.protocol.core.methods.response.EthGetBalance
import kotlin.math.pow

object Web3AuthUtils {

    fun getBlockChainName(blockChain: String): String {
        return when(blockChain) {
            "Ethereum" -> "EthAddress"
            "Solana" -> "SolAddress"
            else -> "EthAddress"
        }
    }

    fun getEtherInWei() = 10.0.pow(18)

    fun getEtherInGwei() = 10.0.pow(17)

    fun toEther(ethBalance: EthGetBalance): Double {
        var decimalWei = ethBalance.balance.toDouble()
        return decimalWei/getEtherInWei()
    }

}