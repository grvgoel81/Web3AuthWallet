package com.web3auth.wallet.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.web3auth.wallet.api.ApiHelper
import com.web3auth.wallet.api.Web3AuthApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService

class EthereumViewModel: ViewModel() {

    private lateinit var web3: Web3j
    private lateinit var web3Balance: EthGetBalance
    var isWeb3Configured = MutableLiveData(false)
    var priceInUSD = MutableLiveData("")
    var publicAddress = MutableLiveData("")
    var balance = MutableLiveData(0.0)


    init {
        configureWeb3j()
    }

    private fun configureWeb3j() {
        val url =
            "https://rpc-mumbai.maticvigil.com/" // Mainnet: https://mainnet.infura.io/v3/{}, 7f287687b3d049e2bea7b64869ee30a3
        web3 = Web3j.build(HttpService(url))
        try {
            val clientVersion: Web3ClientVersion = web3.web3ClientVersion().sendAsync().get()
            isWeb3Configured.value = !clientVersion.hasError()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrencyPriceInUSD(fsym: String, tsyms: String) {
        GlobalScope.launch {
            val web3AuthApi = ApiHelper.getTorusInstance().create(Web3AuthApi::class.java)
            val result = web3AuthApi.getCurrencyPrice(fsym, tsyms)
            if (result.isSuccessful && result.body() != null) {
                priceInUSD.postValue(result.body()?.USD)
            }
        }

    }

    fun getPublicAddress(sessionId: String) {
        GlobalScope.launch {
            val credentials: Credentials = Credentials.create(sessionId)
            publicAddress.postValue(credentials.address)
        }
    }

    fun retrieveBalance(publicAddress: String) {
        GlobalScope.launch {
            web3Balance = web3.ethGetBalance(publicAddress, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get()
            balance.postValue(web3Balance.balance.toDouble())
        }
    }
}