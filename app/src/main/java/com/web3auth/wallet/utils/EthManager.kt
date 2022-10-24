package com.web3auth.wallet.utils

import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService
import java.util.concurrent.Executors

object EthManager {

    private lateinit var web3: Web3j
    private lateinit var credentials: Credentials
    private lateinit var web3Address: String
    private lateinit var web3Balance: EthGetBalance

    fun configureWeb3j(): String {
        val url = "https://small-long-brook.ropsten.quiknode.pro/e2fd2eb01412e80623787d1c40094465aa67624a" // Mainnet: https://mainnet.infura.io/v3/{}, 7f287687b3d049e2bea7b64869ee30a3
        web3 = Web3j.build(HttpService(url))
        try {
            val clientVersion: Web3ClientVersion = web3.web3ClientVersion().sendAsync().get()
            if (clientVersion.hasError()) {
                return "error"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "success"
    }

    fun retrieveBalance(publicAddress: String): String {
        //get wallet's balance
        Executors.newSingleThreadExecutor().execute {
            try {
                web3Balance = web3.ethGetBalance(
                    publicAddress,
                    DefaultBlockParameterName.LATEST
                ).sendAsync().get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return web3Balance.balance.toString() ?: "error"
    }
}