package com.web3auth.wallet.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.guness.ksolana.core.Account
import com.guness.ksolana.core.PublicKey
import com.guness.ksolana.core.Transaction
import com.guness.ksolana.programs.SystemProgram.transfer
import com.guness.ksolana.rpc.Cluster
import com.guness.ksolana.rpc.RpcApi
import com.guness.ksolana.rpc.RpcClient
import com.paymennt.crypto.lib.Base58
import com.paymennt.solanaj.exception.SolanajException
import com.web3auth.wallet.api.ApiHelper
import com.web3auth.wallet.api.Web3AuthApi
import com.web3auth.wallet.utils.MemoProgram
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SolanaViewModel: ViewModel() {

    private lateinit var account: Account
    private lateinit var api: RpcApi
    var priceInUSD = MutableLiveData("")
    var publicAddress = MutableLiveData("")
    var privateKey = MutableLiveData("")
    var balance = MutableLiveData(0L)
    var signature = MutableLiveData("")
    var sendTransactionResult = MutableLiveData(Pair(false, ""))

    fun setNetwork(cluster: Cluster) {
        api =RpcApi(RpcClient(cluster))
    }

    fun getPublicAddress(ed25519Key: String) {
        try {
            var account = Account(Base58.encode(ed25519Key.toByteArray()))
            publicAddress.postValue(account.publicKey.toString())
            privateKey.postValue(account.secretKey.toString())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getCurrencyPriceInUSD(fsym: String, tsyms: String) {
        GlobalScope.launch {
            val web3AuthApi = ApiHelper.getTorusInstance().create(Web3AuthApi::class.java)
            val result = web3AuthApi.getCurrencyPrice(fsym, tsyms)
            if (result.isSuccessful && result.body() != null) {
                priceInUSD.postValue(result.body()?.USD)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getBalance(publicAddress: String) {
        GlobalScope.launch {
            balance.postValue(api.getBalance(PublicKey(publicAddress)))
        }
    }

    fun signTransaction(ed25519Key: String, message: String) {
        GlobalScope.launch {
            var account = Account(Base58.encode(ed25519Key.toByteArray()))
            val transaction = Transaction()
            transaction.addInstruction(MemoProgram.writeUtf8(account.publicKey, message))
            signature.postValue(api.sendTransaction(transaction, account))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun signAndSendTransaction(cluster: Cluster, ed25519Key: String, fromKey: String, toKey: String, amount: Long, message: String?) {
        GlobalScope.launch {
            try {
                var api =RpcApi(RpcClient(cluster))
                var account = Account(Base58.encode(ed25519Key.toByteArray()))
                val fromPublicKey = PublicKey(fromKey)
                val toPublicKey = PublicKey(toKey)
                val transaction = Transaction()
                transaction.addInstruction(transfer(fromPublicKey, toPublicKey, amount))
                if(message?.isNotEmpty() == true) {
                    transaction.addInstruction(MemoProgram.writeUtf8(account.publicKey, message))
                }
                val result = api.sendTransaction(transaction, account)
                if (result.isNotEmpty()) {
                    sendTransactionResult.postValue(Pair(true, result))
                }
            } catch (ex: SolanajException) {
                sendTransactionResult.postValue(Pair(false, ex.message.toString()))
                ex.printStackTrace()
            }
        }
    }
}