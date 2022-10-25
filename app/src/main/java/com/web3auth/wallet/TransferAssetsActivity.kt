package com.web3auth.wallet

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.web3auth.wallet.utils.*
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.math.BigDecimal

class TransferAssetsActivity: AppCompatActivity() {

    private lateinit var etReceiptentAddress: AppCompatEditText
    private lateinit var etAmountToSend: AppCompatEditText
    private lateinit var etMaxTransFee: AppCompatEditText
    private lateinit var web3: Web3j
    private lateinit var publicAddress: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_asset)
        supportActionBar?.hide()
        setUpListeners()
        configureWeb3j()
    }

    private fun setUpListeners() {
        etReceiptentAddress = findViewById(R.id.etReceiptentAddress)
        etAmountToSend = findViewById(R.id.etAmountToSend)
        etMaxTransFee = findViewById(R.id.etMaxTransFee)
        val etBlockChain = findViewById<AppCompatEditText>(R.id.etBlockChain)
        val etBlockChainAdd = findViewById<AppCompatEditText>(R.id.etBlockChainAdd)
        val blockChain = Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(BLOCKCHAIN, "Ethereum")
        publicAddress = Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(PUBLICKEY, "").toString()

        etBlockChainAdd.setText(blockChain?.let { Web3AuthUtils.getBlockChainName(it) })
        etBlockChain.setText(blockChain.toString())
        findViewById<AppCompatImageView>(R.id.ivScan).setOnClickListener { scanQRCode() }
        findViewById<AppCompatButton>(R.id.btnTransfer).setOnClickListener {
            showConfirmTransactionDialog(publicAddress, etReceiptentAddress.text.toString(), etAmountToSend.text.toString().toDouble(),
            etMaxTransFee.text.toString())
        }
    }

    private fun configureWeb3j() {
        val url = "https://rpc-mumbai.maticvigil.com/" // Mainnet: https://mainnet.infura.io/v3/{}, 7f287687b3d049e2bea7b64869ee30a3
        web3 = Web3j.build(HttpService(url))
        try {
            val clientVersion: Web3ClientVersion = web3.web3ClientVersion().sendAsync().get()
            if (clientVersion.hasError()) {
                toast("Error connecting to Web3j")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanQRCode() {
        barcodeLauncher.launch(ScanOptions())
    }

    private val barcodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            val originalIntent = result.originalIntent
            if (originalIntent == null) {
                Toast.makeText(this@TransferAssetsActivity, "Cancelled", Toast.LENGTH_LONG).show()
            } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                Toast.makeText(
                    this@TransferAssetsActivity,
                    "Cancelled due to missing camera permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            etReceiptentAddress.setText(result.contents)
        }
    }

    @Throws(java.lang.Exception::class)
    fun makeTransaction(privateKey: String, recipientAddress: String, amountToBeSent: Double) {
        try {
            val credentials: Credentials = Credentials.create(privateKey)
            val receipt = Transfer.sendFunds(web3,
                credentials,
                recipientAddress,
                BigDecimal.valueOf(amountToBeSent),
                Convert.Unit.ETHER
            ).send()
            toast("Transaction successful: " + receipt.transactionHash)
        } catch (e: java.lang.Exception) {
            toast("low balance")
        }
    }

    private fun showConfirmTransactionDialog(senderAdd: String, receiptAdd: String, amountToBeSent: Double, maxTransactionFee: String) {
        val dialog = Dialog(this@TransferAssetsActivity)
        dialog.setContentView(R.layout.dialog_confirm_transaction)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.animation
        val tvNetwork = dialog.findViewById<AppCompatTextView>(R.id.tvNetwork)
        val tvSenderAdd = dialog.findViewById<AppCompatTextView>(R.id.tvSenderAdd)
        val tvReceiptAdd = dialog.findViewById<AppCompatTextView>(R.id.tvReceiptAdd)
        val tvAmountValue = dialog.findViewById<AppCompatTextView>(R.id.tvAmountValue)
        val tvExchangePrice = dialog.findViewById<AppCompatTextView>(R.id.tvExchangePrice)
        val tvTransactionValue = dialog.findViewById<AppCompatTextView>(R.id.tvTransactionValue)
        val tvTotalAmount = dialog.findViewById<AppCompatTextView>(R.id.tvTotalAmount)
        val tvTotalCostInUSD = dialog.findViewById<AppCompatTextView>(R.id.tvTotalCostInUSD)
        val tvExchangeTime = dialog.findViewById<AppCompatTextView>(R.id.tvExchangeTime)
        val tvCancel = dialog.findViewById<AppCompatTextView>(R.id.tvCancel)
        val btnConfirm = dialog.findViewById<AppCompatButton>(R.id.btnConfirm)

        tvSenderAdd.text = senderAdd
        tvReceiptAdd.text = receiptAdd
        tvAmountValue.text = amountToBeSent.toString()
        tvTransactionValue.text = maxTransactionFee

        val selectedNetwork = Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(NETWORK, "Mainnet").toString()
        val blockChain = Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(BLOCKCHAIN, "Ethereum")
        tvNetwork.text = blockChain.plus(" ").plus(selectedNetwork)


        btnConfirm.setOnClickListener {
            showTransactionDialog(TransactionStatus.PLACED)
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showTransactionDialog(transactionStatus: TransactionStatus) {
        val dialog = Dialog(this@TransferAssetsActivity)
        dialog.setContentView(R.layout.popup_transaction)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.animation
        val ivState = dialog.findViewById<AppCompatImageView>(R.id.ivState)
        val transactionState = dialog.findViewById<AppCompatTextView>(R.id.tvTransactionState)
        val tvStatus = dialog.findViewById<AppCompatTextView>(R.id.tvStatus)
        val ivClose = dialog.findViewById<AppCompatImageView>(R.id.ivClose)

        when (transactionStatus) {
            TransactionStatus.PLACED -> {
                transactionState.text = getString(R.string.transaction_placed)
                ivState.setImageDrawable(getDrawable(R.drawable.ic_transaction_placed))
                tvStatus.hide()
            }
            TransactionStatus.SUCCESSFUL -> {
                transactionState.text = getString(R.string.transaction_success)
                ivState.setImageDrawable(getDrawable(R.drawable.ic_iv_transaction_success))
            }
            TransactionStatus.FAILED -> {
                transactionState.text = getString(R.string.transaction_failed)
                ivState.setImageDrawable(getDrawable(R.drawable.ic_transaction_failed))
                tvStatus.text = getString(R.string.try_again)
            }
            else -> {
                transactionState.text = getString(R.string.transaction_pending)
                ivState.setImageDrawable(getDrawable(R.drawable.ic_transaction_pending))
            }
        }

        ivClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}