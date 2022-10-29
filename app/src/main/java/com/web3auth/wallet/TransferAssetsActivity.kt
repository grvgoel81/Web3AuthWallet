package com.web3auth.wallet

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.postDelayed
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.web3auth.wallet.api.ApiHelper
import com.web3auth.wallet.api.Web3AuthApi
import com.web3auth.wallet.api.models.EthGasAPIResponse
import com.web3auth.wallet.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger

class TransferAssetsActivity : AppCompatActivity() {

    private lateinit var etReceiptentAddress: AppCompatEditText
    private lateinit var etAmountToSend: AppCompatEditText
    private lateinit var etMaxTransFee: AppCompatEditText
    private lateinit var web3: Web3j
    private lateinit var publicAddress: String
    private lateinit var ethGasAPIResponse: EthGasAPIResponse
    private lateinit var blockChain: String
    private lateinit var network: String
    private lateinit var tvEth: AppCompatTextView
    private lateinit var tvUSD: AppCompatTextView
    private lateinit var tvTotalAmount: AppCompatTextView
    private lateinit var tvCostInETH: AppCompatTextView
    private lateinit var sessionID: String
    private lateinit var priceInUSD: String
    private var totalCostinETH: Double = 0.0
    private var gasFee: Double = 0.0
    private var processTime:Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_asset)
        supportActionBar?.hide()
        setUpListeners()
        getMaxTransactionConfig()
        configureWeb3j()
    }

    private fun setUpListeners() {
        etReceiptentAddress = findViewById(R.id.etReceiptentAddress)
        etAmountToSend = findViewById(R.id.etAmountToSend)
        etMaxTransFee = findViewById(R.id.etMaxTransFee)
        tvEth = findViewById(R.id.tvEth)
        tvUSD = findViewById(R.id.tvUSD)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvCostInETH = findViewById(R.id.tvCostInETH)
        val etBlockChain = findViewById<AppCompatEditText>(R.id.etBlockChain)
        val etBlockChainAdd = findViewById<AppCompatEditText>(R.id.etBlockChainAdd)
        blockChain = Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.getString(BLOCKCHAIN, "Ethereum").toString()
        network = Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.getString(NETWORK, "Mainnet").toString()
        publicAddress = Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.getString(PUBLICKEY, "").toString()
        sessionID = Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.getString(SESSION_ID, "").toString()
        priceInUSD = Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.getString(PRICE_IN_USD, "").toString()

        etBlockChainAdd.setText(blockChain.let { Web3AuthUtils.getBlockChainName(it) })
        etBlockChain.setText(blockChain)

        findViewById<AppCompatImageView>(R.id.ivBack).setOnClickListener { onBackPressed() }
        findViewById<AppCompatImageView>(R.id.ivScan).setOnClickListener { scanQRCode() }

        tvEth.setOnClickListener {
            it.background = getDrawable(R.drawable.bg_layout_tv_blue)
            tvUSD.background = getDrawable(R.drawable.bg_layout_tv_grey)
        }

        tvUSD.setOnClickListener {
            it.background = getDrawable(R.drawable.bg_layout_tv_blue)
            tvEth.background = getDrawable(R.drawable.bg_layout_tv_grey)
        }

        etAmountToSend.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(s?.isNotEmpty() == true) {
                    totalCostinETH = s.toString().toDouble()
                        .plus(Web3AuthUtils.getMaxTransactionFee(ethGasAPIResponse.fastest))
                    setTotalAmount()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<AppCompatButton>(R.id.btnTransfer).setOnClickListener {
            //signMessage(Web3AuthUtils.getPrivateKey(sessionID), "0xE84D601E5D945031129a83E5602be0CC7f182Cf3"/*etReceiptentAddress.text.toString()*/, 0.00012)
            if (isValidDetails()) {
                showConfirmTransactionDialog(
                    publicAddress,
                    etReceiptentAddress.text.toString(),
                    etAmountToSend.text.toString(),
                    gasFee, processTime,
                    tvTotalAmount.text.toString(),
                    tvCostInETH.text.toString()
                )
            }
        }

        findViewById<AppCompatTextView>(R.id.tvEditTransFee).setOnClickListener {
            showMaxTransactionSelectDialog()
        }
    }

    private fun configureWeb3j() {
        val url =
            "https://rpc-mumbai.maticvigil.com/" // Mainnet: https://mainnet.infura.io/v3/{}, 7f287687b3d049e2bea7b64869ee30a3
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

    private fun getMaxTransactionConfig() {
        GlobalScope.launch {
            val web3AuthApi =
                ApiHelper.getEthInstance().create(Web3AuthApi::class.java)
            val result = web3AuthApi.getMaxTransactionConfig()
            if (result.isSuccessful && result.body() != null) {
                Handler(Looper.getMainLooper()).postDelayed(10) {
                    ethGasAPIResponse = result.body()!!
                    setMaxTransFee(ethGasAPIResponse.fastest)
                    gasFee = ethGasAPIResponse.fastest
                    processTime = ethGasAPIResponse.fastestWait
                }
            }
        }
    }

    private fun isValidDetails(): Boolean {
        return if(etReceiptentAddress.text?.isNullOrEmpty() == true) {
            toast(getString(R.string.enter_address))
            false
        } /*else if (Web3AuthUtils.isValidEthAddress(etReceiptentAddress.text.toString())) {
            toast(getString(R.string.correct_address))
            false
        } */else if(etAmountToSend.text?.isNullOrEmpty() == true) {
            toast(getString(R.string.enter_amt_to_transfer))
            false
        } else {
            true
        }
    }

    @Throws(java.lang.Exception::class)
    fun makeTransaction(privateKey: String, recipientAddress: String,
                        amountToBeSent: Double) {
        GlobalScope.launch {
            try {
                val credentials: Credentials = Credentials.create(privateKey)
                val receipt = Transfer.sendFunds(
                    web3,
                    credentials,
                    recipientAddress,
                    BigDecimal.valueOf(amountToBeSent),
                    Convert.Unit.ETHER
                ).send()
                Handler(Looper.getMainLooper()).postDelayed(10) {
                    toast("Transaction successful: " + receipt.transactionHash)
                }
            } catch (e: java.lang.Exception) {
                Handler(Looper.getMainLooper()).postDelayed(10) {
                    toast("low balance")
                }
            }
        }
    }

    private fun showMaxTransactionSelectDialog() {
        val dialog = Dialog(this@TransferAssetsActivity)
        dialog.setContentView(R.layout.dialog_max_transaction_fee)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.animation
        val clFast = dialog.findViewById<ConstraintLayout>(R.id.clFast)
        val clAvg = dialog.findViewById<ConstraintLayout>(R.id.clAvg)
        val clSlow = dialog.findViewById<ConstraintLayout>(R.id.clSlow)
        val rbFast = dialog.findViewById<RadioButton>(R.id.rbFast)
        val rbAvg = dialog.findViewById<RadioButton>(R.id.rbAvg)
        val rbSlow = dialog.findViewById<RadioButton>(R.id.rbSlow)
        val tvFastProcessTime = dialog.findViewById<AppCompatTextView>(R.id.tvFastProcessTime)
        val tvAvgProcessTime = dialog.findViewById<AppCompatTextView>(R.id.tvAvgProcessTime)
        val tvSlowProcessTime = dialog.findViewById<AppCompatTextView>(R.id.tvSlowProcessTime)
        val tvFastEth = dialog.findViewById<AppCompatTextView>(R.id.tvFastEth)
        val tvSlowEth = dialog.findViewById<AppCompatTextView>(R.id.tvSlowEth)
        val tvAvgEth = dialog.findViewById<AppCompatTextView>(R.id.tvAvgEth)
        val tvCancel = dialog.findViewById<AppCompatTextView>(R.id.tvCancel)
        val btnSave = dialog.findViewById<AppCompatButton>(R.id.btnSave)

        tvFastProcessTime.text = getString(R.string.process_in).plus(" ")
            .plus(Web3AuthUtils.convertMinsToSec(ethGasAPIResponse.fastestWait)).plus(" ").plus(getString(R.string.seconds))
        tvAvgProcessTime.text = getString(R.string.process_in).plus(" ")
            .plus(Web3AuthUtils.convertMinsToSec(ethGasAPIResponse.fastWait)).plus(" ").plus(getString(R.string.seconds))
        tvSlowProcessTime.text = getString(R.string.process_in).plus(" ")
            .plus(Web3AuthUtils.convertMinsToSec(ethGasAPIResponse.avgWait)).plus(" ").plus(getString(R.string.seconds))
        tvFastEth.text = getString(R.string.upto).plus(" ").plus(Web3AuthUtils.getMaxTransactionFee(ethGasAPIResponse.fastest))
            .plus(" ").plus(Web3AuthUtils.getCurrency(blockChain))
        tvAvgEth.text = getString(R.string.upto).plus(" ").plus(Web3AuthUtils.getMaxTransactionFee(ethGasAPIResponse.fast))
            .plus(" ").plus(Web3AuthUtils.getCurrency(blockChain))
        tvSlowEth.text = getString(R.string.upto).plus(" ").plus(Web3AuthUtils.getMaxTransactionFee(ethGasAPIResponse.average))
            .plus(" ").plus(Web3AuthUtils.getCurrency(blockChain))

        clFast.setOnClickListener {
            rbAvg.isChecked = false
            rbSlow.isChecked = false
            dialog.dismiss()
            gasFee = ethGasAPIResponse.fastest
            processTime = ethGasAPIResponse.fastestWait
            setMaxTransFee(ethGasAPIResponse.fastest)
        }

        clAvg.setOnClickListener {
            rbFast.isChecked = false
            rbSlow.isChecked = false
            dialog.dismiss()
            gasFee = ethGasAPIResponse.fast
            processTime = ethGasAPIResponse.fastWait
            setMaxTransFee(ethGasAPIResponse.fast)
        }

        clSlow.setOnClickListener {
            rbAvg.isChecked = false
            rbFast.isChecked = false
            dialog.dismiss()
            gasFee = ethGasAPIResponse.average
            processTime = ethGasAPIResponse.avgWait
            setMaxTransFee(ethGasAPIResponse.average)
        }

        btnSave.setOnClickListener {
            dialog.dismiss()
        }
        tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showConfirmTransactionDialog(senderAdd: String, receiptAdd: String, amountToBeSent: String,
        gasFee: Double, processTime: Double, totalAmountInEth: String, totalAmountInUSD: String
    ) {
        val transDialog = Dialog(this@TransferAssetsActivity)
        transDialog.setContentView(R.layout.dialog_confirm_transaction)
        transDialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        transDialog.setCancelable(false)
        transDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        transDialog.window?.attributes?.windowAnimations = R.style.animation
        val clTransaction = transDialog.findViewById<ConstraintLayout>(R.id.clTransaction)
        val clProgressBar = transDialog.findViewById<ConstraintLayout>(R.id.clProgressBar)
        val tvNetwork = transDialog.findViewById<AppCompatTextView>(R.id.tvNetwork)
        val tvSenderAdd = transDialog.findViewById<AppCompatTextView>(R.id.tvSenderAdd)
        val tvReceiptAdd = transDialog.findViewById<AppCompatTextView>(R.id.tvReceiptAdd)
        val tvAmountInETH = transDialog.findViewById<AppCompatTextView>(R.id.tvAmountinETH)
        val tvAmountInUsd = transDialog.findViewById<AppCompatTextView>(R.id.tvAmountInUsd)
        val tvTransactionValue = transDialog.findViewById<AppCompatTextView>(R.id.tvTransactionValue)
        val tvTotalAmountInETH = transDialog.findViewById<AppCompatTextView>(R.id.tvTotalAmountInETH)
        val tvTotalCostInUSD = transDialog.findViewById<AppCompatTextView>(R.id.tvTotalCostInUSD)
        val tvProcessTime = transDialog.findViewById<AppCompatTextView>(R.id.tvProcessTime)
        val tvCancel = transDialog.findViewById<AppCompatTextView>(R.id.tvCancel)
        val btnConfirm = transDialog.findViewById<AppCompatButton>(R.id.btnConfirm)

        tvSenderAdd.text = senderAdd
        tvReceiptAdd.text = receiptAdd
        tvAmountInETH.text = amountToBeSent.plus(" " + Web3AuthUtils.getCurrency(blockChain))
        tvAmountInUsd.text = "~".plus(String.format("%.6f", Web3AuthUtils.getPriceinUSD(amountToBeSent.toDouble(), priceInUSD.toDouble())))
            .plus(" ").plus(getString(R.string.usd))
        tvNetwork.text = blockChain.plus(" ").plus(network)
        tvTransactionValue.text = Web3AuthUtils.getMaxTransactionFee(gasFee).toString()
            .plus(" " + Web3AuthUtils.getCurrency(blockChain))
        tvProcessTime.text = "in".plus(" ").plus(" ")
            .plus(Web3AuthUtils.convertMinsToSec(processTime)).plus(" ").plus(getString(R.string.seconds))

        tvTotalAmountInETH.text = totalAmountInEth
        tvTotalCostInUSD.text = totalAmountInUSD

        btnConfirm.setOnClickListener {
            clTransaction.hide()
            clProgressBar.show()
            signMessage(transDialog, Web3AuthUtils.getPrivateKey(sessionID), receiptAdd, totalAmountInEth.split(" ")[0].toDouble())
            //showTransactionDialog(TransactionStatus.PLACED)
        }
        tvCancel.setOnClickListener {
            transDialog.dismiss()
        }
        transDialog.show()
    }

    private fun setMaxTransFee(fee: Double) {
        etMaxTransFee.setText(getString(R.string.upto).plus(" ").plus(Web3AuthUtils.getMaxTransactionFee(fee)))
        totalCostinETH = if(etAmountToSend.text?.isNotEmpty() == true) {
            etAmountToSend.text.toString().toDouble()
                .plus(Web3AuthUtils.getMaxTransactionFee(fee))
        } else {
            Web3AuthUtils.getMaxTransactionFee(fee)
        }
        setTotalAmount()
    }

    private fun setTotalAmount() {
        tvTotalAmount.text =
            String.format("%.6f", totalCostinETH).plus(" " + Web3AuthUtils.getCurrency(blockChain))
        tvCostInETH.text = "= ".plus(String.format("%.6f", Web3AuthUtils.getPriceinUSD(totalCostinETH, priceInUSD.toDouble())))
            .plus(" ").plus(getString(R.string.usd))
    }

    private fun signMessage(transDialog: Dialog, privateKey: String, recipientAddress: String, amountToBeSent: Double) {
        GlobalScope.launch {
            try {
                val credentials: Credentials = Credentials.create(privateKey)
                println("Account address: " + credentials.address)
                println(
                    "Balance: " + Convert.fromWei(
                        web3.ethGetBalance(credentials.address, DefaultBlockParameterName.LATEST)
                            .send().balance.toString(), Convert.Unit.ETHER
                    )
                )
                val ethGetTransactionCount: EthGetTransactionCount = web3.ethGetTransactionCount(
                    credentials.address,
                    DefaultBlockParameterName.LATEST
                ).send()
                val nonce: BigInteger = ethGetTransactionCount.transactionCount
                val value: BigInteger =
                    Convert.toWei(amountToBeSent.toString(), Convert.Unit.ETHER).toBigInteger()
                val gasLimit: BigInteger = BigInteger.valueOf(21000)
                val gasPrice: BigInteger =  Web3AuthUtils.convertMinsToSec(ethGasAPIResponse.avgWait).toBigDecimal().toBigInteger()

                val rawTransaction: RawTransaction = RawTransaction.createTransaction(80001,
                    nonce,
                    gasLimit,
                    recipientAddress,
                    value,
                    "" , BigInteger.valueOf(1), BigInteger.valueOf(35)
                )
                // Sign the transaction
                val signedMessage: ByteArray =
                    TransactionEncoder.signMessage(rawTransaction, credentials)
                val hexValue: String = Numeric.toHexString(signedMessage)
                println("kexValue: $hexValue")
                val ethSendTransaction: EthSendTransaction =
                    web3.ethSendRawTransaction(hexValue).send()
                val transactionHash: String = ethSendTransaction.transactionHash
                println("transactionHash: $transactionHash")
                runOnUiThread {
                    showTransactionDialog(TransactionStatus.SUCCESSFUL)
                    transDialog.dismiss()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                runOnUiThread {
                    showTransactionDialog(TransactionStatus.FAILED)
                    transDialog.dismiss()
                }
            }
        }
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
        val icCLose = dialog.findViewById<AppCompatImageView>(R.id.icCLose)

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

        icCLose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}