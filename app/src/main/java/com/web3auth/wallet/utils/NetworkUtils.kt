package com.web3auth.wallet.utils

import com.guness.ksolana.rpc.Cluster
import com.web3auth.core.Web3Auth
import org.torusresearch.fetchnodedetails.types.TorusNetwork

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

    fun getSolanaNetwork(network:String): Cluster {
        return when(network) {
            "Mainnet" -> Cluster.MAINNET
            "Testnet" -> Cluster.TESTNET
            else  -> Cluster.DEVNET
        }
    }
}