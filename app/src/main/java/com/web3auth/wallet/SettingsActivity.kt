package com.web3auth.wallet

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.web3auth.wallet.utils.BLOCKCHAIN
import com.web3auth.wallet.utils.NETWORK
import com.web3auth.wallet.utils.set
import com.web3auth.wallet.utils.web3AuthWalletPreferences

class SettingsActivity : AppCompatActivity() {

    private lateinit var spBlockChain: AutoCompleteTextView
    private lateinit var tvNetwork: AppCompatTextView
    private lateinit var blockChain: String
    private lateinit var network: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.hide()

        blockChain = Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getString(BLOCKCHAIN, "ETH Mainnet").toString()
        network = Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getString(NETWORK, "Mainnet").toString()
        setData()
        setUpSpinner()
    }

    private fun setUpSpinner() {
        spBlockChain.setText(blockChain)

        var blockchains = resources.getStringArray(R.array.blockchains)
        val adapter: ArrayAdapter<String> =
            ArrayAdapter(this, R.layout.item_dropdown, blockchains)
        spBlockChain.setAdapter(adapter)
        spBlockChain.setOnItemClickListener { _, _, position, _ ->
            Web3AuthWalletApp.getContext().web3AuthWalletPreferences[BLOCKCHAIN] = blockchains[position]
            tvNetwork.text = blockchains[position]
        }
    }

    private fun setData() {
        spBlockChain = findViewById(R.id.spBlockChain)
        tvNetwork = findViewById(R.id.tvNetwork)
        findViewById<AppCompatImageView>(R.id.ivBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        tvNetwork.text = blockChain
    }
}