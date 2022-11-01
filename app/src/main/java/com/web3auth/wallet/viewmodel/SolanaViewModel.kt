package com.web3auth.wallet.viewmodel

import androidx.lifecycle.ViewModel
import com.paymennt.crypto.bip32.Network
import com.paymennt.crypto.bip32.wallet.AbstractWallet
import com.paymennt.solanaj.api.rpc.Cluster
import com.paymennt.solanaj.api.rpc.SolanaRpcClient
import com.paymennt.solanaj.data.SolanaAccount
import com.paymennt.solanaj.data.SolanaPublicKey
import com.paymennt.solanaj.data.SolanaTransaction
import com.paymennt.solanaj.program.SystemProgram
import com.paymennt.solanaj.wallet.SolanaWallet
import com.web3auth.wallet.utils.SolanaManager

class SolanaViewModel: ViewModel() {

    private lateinit var solanaWallet: SolanaWallet

    init {
        val network = Network.TESTNET
        // create wallet
        SolanaManager.solanaWallet = SolanaWallet("", "", network)

        // get address (account, chain, index), used to receive
        SolanaManager.solanaWallet.getAddress(0, AbstractWallet.Chain.EXTERNAL, null)

        // get private key (account, chain, index), used to sign transactions
        SolanaManager.solanaWallet.getPrivateKey(0, AbstractWallet.Chain.EXTERNAL, null)
    }

    fun getBalance(publicAddress: String): Long {
        val client = SolanaRpcClient(Cluster.DEVNET)
        return client.api.getBalance(publicAddress)
    }

    fun signAndSendTransaction(receiverAddress: String, amount: Long): String {
        // create new SolanaRpcClient, (DEVNET, TESTNET, MAINNET)
        val client = SolanaRpcClient(Cluster.DEVNET)

        // amount to transfer in lamports, 1 SOL = 1000000000 lamports
        //val amount: Long = 1000000000

        // create new transaction
        val transaction = SolanaTransaction()

        // create solana account, this account holds the funds that we want to transfer
        val account =
            SolanaAccount(SolanaManager.solanaWallet.getPrivateKey(0, AbstractWallet.Chain.EXTERNAL, null))

        // define the sender and receiver public keys
        val fromPublicKey: SolanaPublicKey = account.publicKey
        val toPublickKey = SolanaPublicKey(receiverAddress)

        // add instructions to the transaction (from, to, lamports)
        transaction.addInstruction(SystemProgram.transfer(fromPublicKey, toPublickKey, amount))

        // set the recent blockhash
        transaction.setRecentBlockHash(client.api.recentBlockhash)

        // set the fee payer
        transaction.feePayer = account.publicKey

        // sign the transaction
        transaction.sign(account)

        // publish the transaction
        return client.api.sendTransaction(transaction)
    }
}