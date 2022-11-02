package com.web3auth.wallet.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.paymennt.crypto.bip32.Network
import com.paymennt.crypto.bip32.wallet.AbstractWallet
import com.paymennt.solanaj.api.rpc.Cluster
import com.paymennt.solanaj.api.rpc.SolanaRpcClient
import com.paymennt.solanaj.data.SolanaAccount
import com.paymennt.solanaj.data.SolanaMessage
import com.paymennt.solanaj.data.SolanaPublicKey
import com.paymennt.solanaj.data.SolanaTransaction
import com.paymennt.solanaj.program.SystemProgram
import com.paymennt.solanaj.wallet.SolanaWallet
import com.web3auth.wallet.api.ApiHelper
import com.web3auth.wallet.api.Web3AuthApi
import com.web3auth.wallet.utils.SolanaManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.pow

class SolanaViewModel: ViewModel() {

    private var solanaWallet: SolanaWallet
    var priceInUSD = MutableLiveData("")
    var publicAddress = MutableLiveData("")
    var privateKey = MutableLiveData("")
    var balance = MutableLiveData(0L)
    var sendTransactionResult = MutableLiveData(Pair(false, ""))

    init {
        val network = Network.TESTNET
        solanaWallet = SolanaWallet("swing brown giraffe enter common awful rent shock mobile wisdom increase banana",
            "", network)

        // get address (account, chain, index), used to receive
        publicAddress.postValue(solanaWallet.getAddress(0, AbstractWallet.Chain.EXTERNAL, null))

        // get private key (account, chain, index), used to sign transactions
        privateKey.postValue(solanaWallet.getPrivateKey(0, AbstractWallet.Chain.EXTERNAL, null).toString())
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

    fun getBalance(publicAddress: String) {
        GlobalScope.launch {
            val client = SolanaRpcClient(Cluster.TESTNET)
            balance.postValue(client.api.getBalance(publicAddress))
        }
    }

    fun signAndSendTransaction(receiverAddress: String, amount: Long) {
        GlobalScope.launch {
            try {
                val client = SolanaRpcClient(Cluster.TESTNET)

                // amount to transfer in lamports, 1 SOL = 1000000000 lamports
                //val amount: Long = 1000000000

                val transaction = SolanaTransaction()

                // create solana account, this account holds the funds that we want to transfer
                val account =
                    SolanaAccount(
                        solanaWallet.getPrivateKey(
                            0,
                            AbstractWallet.Chain.EXTERNAL,
                            null
                        )
                    )

                val fromPublicKey: SolanaPublicKey = account.publicKey
                val toPublickKey = SolanaPublicKey(receiverAddress)

                transaction.addInstruction(
                    SystemProgram.transfer(
                        fromPublicKey,
                        toPublickKey,
                        amount
                    )
                )
                // set the recent blockhash
                transaction.setRecentBlockHash(client.api.recentBlockhash)
                transaction.feePayer = account.publicKey
                // sign the transaction
                transaction.sign(account)
                // publish the transaction
                val result = client.api.sendTransaction(transaction)
                if (result.isNotEmpty()) {
                    sendTransactionResult.postValue(Pair(true, result))
                }
            } catch (ex: Exception) {
                sendTransactionResult.postValue(Pair(false, ex.toString()))
                ex.printStackTrace()
            }
        }
    }
}