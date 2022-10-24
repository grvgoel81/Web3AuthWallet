package com.web3auth.wallet.utils

import com.web3auth.core.Web3Auth
import org.torusresearch.fetchnodedetails.types.TorusNetwork
import com.paymennt.crypto.bip32.Network

object NetworkUtils {

    fun getWebAuthNetwork(network: String): Web3Auth.Network {
        return when(network) {
            "Mainnet" -> Web3Auth.Network.MAINNET
            "Testnet" -> Web3Auth.Network.TESTNET
            else  -> Web3Auth.Network.CYAN
        }
    }

    fun getTorusNetwork(network: String): TorusNetwork {
        return when(network) {
            "Mainnet" -> TorusNetwork.MAINNET
            "Testnet" -> TorusNetwork.TESTNET
            else  -> TorusNetwork.CYAN
        }
    }

    fun getSolanaNetwork(network:String): Network {
        return when(network) {
            "Mainnet" -> Network.MAINNET
            "Testnet" -> Network.TESTNET
            else  -> Network.TESTNET
        }
    }
}