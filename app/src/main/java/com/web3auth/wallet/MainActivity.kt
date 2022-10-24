package com.web3auth.wallet

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.web3auth.core.Web3Auth
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.WhiteLabelData
import com.web3auth.wallet.utils.*
import org.torusresearch.fetchnodedetails.types.TorusNetwork
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var web3Auth: Web3Auth
    private var web3AuthResponse: Web3AuthResponse? = null
    private lateinit var web3: Web3j
    private lateinit var credentials: Credentials
    private lateinit var web3Address: String
    private lateinit var web3Balance: EthGetBalance
    private lateinit var selectedNetwork: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        selectedNetwork = Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(NETWORK, "Mainnet").toString()
        configureWeb3j()
        configureWeb3Auth()
        setData()
        setUpListeners()
    }

    private fun configureWeb3Auth() {

        Web3Auth(
            Web3AuthOptions(context = this,
                clientId = getString(R.string.web3auth_project_id),
                network =  NetworkUtils.getWebAuthNetwork(selectedNetwork),
                redirectUrl = Uri.parse("torusapp://org.torusresearch.web3authexample/redirect"),
                whiteLabel = WhiteLabelData(
                    "Web3Auth Sample App", null, null, "en", true,
                    hashMapOf(
                        "primary" to "#123456"
                    )
                )
            )
        ).also { web3Auth = it }

        web3Auth.setResultUrl(intent.data)
        web3AuthResponse = Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getObject(LOGIN_RESPONSE)
        web3AuthResponse?.let { getEthAddress(it) }

        findViewById<AppCompatTextView>(R.id.tvName).text = "Welcome ".plus(
            web3AuthResponse?.userInfo?.name?.split(" ")?.get(0)
        ).plus("!")
        findViewById<AppCompatTextView>(R.id.tvEmail).text = web3AuthResponse?.userInfo?.email
    }

    private fun configureWeb3j() {
        val url = "https://small-long-brook.ropsten.quiknode.pro/e2fd2eb01412e80623787d1c40094465aa67624a" // Mainnet: https://mainnet.infura.io/v3/{}, 7f287687b3d049e2bea7b64869ee30a3
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

    private fun setUpListeners() {
        findViewById<AppCompatButton>(R.id.btnTransfer).setOnClickListener {
            startActivity(Intent(this@MainActivity, TransferAssetsActivity::class.java))
        }

        findViewById<AppCompatImageView>(R.id.ivLogout).setOnClickListener { logout() }
        findViewById<AppCompatTextView>(R.id.tvLogout).setOnClickListener { logout() }

        findViewById<AppCompatImageView>(R.id.ivQRCode).setOnClickListener {
            showQR_Dialog(web3Address)
        }

        findViewById<AppCompatButton>(R.id.btnSign).setOnClickListener {
            showSignTransactionDialog(false, "0xsdfgfgsdfngsdfjhdbfknasvhbksdnjkadbsg2345hbhb3b45jbbb4b3bh5j3hhh3khjh3knjj3nkh5jh3kh6hk3jbg5jn4jb6j4")
        }
    }

    private fun setData() {
        val blockChain = Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(BLOCKCHAIN, "Ethereum")
        findViewById<AppCompatTextView>(R.id.tvNetwork).text = blockChain.plus(" ").plus(selectedNetwork)

        /*if(blockChain == getString(R.string.ethereum)) {
            EthManager.configureWeb3j()
        } else {
            SolanaManager.createWallet(NetworkUtils.getSolanaNetwork(selectedNetwork))
        }*/
    }

    private fun getEthAddress(web3AuthResponse: Web3AuthResponse) {
        val authArgs = CustomAuthArgs(NetworkUtils.getTorusNetwork(selectedNetwork))
        authArgs.networkUrl = "https://small-long-brook.ropsten.quiknode.pro/e2fd2eb01412e80623787d1c40094465aa67624a"
        // Initialize CustomAuth
        var torusSdk = CustomAuth(authArgs)
        val verifier = web3AuthResponse.userInfo?.verifier
        val verifierId = web3AuthResponse.userInfo?.verifierId

        Executors.newSingleThreadExecutor().execute {
            web3Address = torusSdk.getEthAddress(verifier, verifierId)
            runOnUiThread {
                findViewById<AppCompatTextView>(R.id.tvAddress).text = web3Address.take(3).plus("...").plus(web3Address.takeLast(4))
                Web3AuthApp.getContext()?.web3AuthWalletPreferences?.set(ETH_Address, web3Address)
                retrieveBalance(web3Address)
            }
        }
    }

    private fun retrieveBalance(publicAddress: String) {
        //get wallet's balance
        Executors.newSingleThreadExecutor().execute {
            try {
                web3Balance = web3.ethGetBalance(
                    publicAddress,
                    DefaultBlockParameterName.LATEST
                ).sendAsync()
                    .get()
            } catch (e: Exception) {
                toast("balance failed")
            }
            runOnUiThread {
                val tvBalance = findViewById<AppCompatTextView>(R.id.tvBalance)
                tvBalance.text = web3Balance.balance.toString()
            }
        }
    }

    private fun signMessage(privetKey: String, recipientAddress: String, amountToBeSent: Double) {
        try {
            val credentials: Credentials = Credentials.create(privetKey)
            println("Account address: " + credentials.address)
            println(
                "Balance: " + Convert.fromWei(
                    web3.ethGetBalance(credentials.address, DefaultBlockParameterName.LATEST)
                        .send().balance.toString(), Convert.Unit.ETHER
                )
            )
            val ethGetTransactionCount: EthGetTransactionCount = web3
                .ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST)
                .send()
            val nonce: BigInteger = ethGetTransactionCount.transactionCount
            val value: BigInteger =
                Convert.toWei(amountToBeSent.toString(), Convert.Unit.ETHER).toBigInteger()
            val gasLimit: BigInteger = BigInteger.valueOf(21000)
            val gasPrice: BigInteger = Convert.toWei("1", Convert.Unit.GWEI).toBigInteger()

            val rawTransaction: RawTransaction = RawTransaction.createEtherTransaction(
                nonce, gasPrice, gasLimit,
                recipientAddress, value
            )
            // Sign the transaction
            val signedMessage: ByteArray = TransactionEncoder.signMessage(rawTransaction, credentials)
            val hexValue: String = Numeric.toHexString(signedMessage)
            val ethSendTransaction: EthSendTransaction = web3.ethSendRawTransaction(hexValue).send()
            val transactionHash: String = ethSendTransaction.transactionHash
            println("transactionHash: $transactionHash")
            showSignTransactionDialog(true, transactionHash)
        } catch (ex: Exception) { ex.printStackTrace() }
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
            println("low balance")
        }
    }

    private fun showQR_Dialog(publicAddress: String) {
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.dialog_qr_code)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.window?.attributes?.windowAnimations = R.style.animation
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val ivQR = dialog.findViewById<AppCompatImageView>(R.id.ivQRCode)
        val tvAddress = dialog.findViewById<AppCompatTextView>(R.id.tvAddress)
        val ivClose = dialog.findViewById<AppCompatImageView>(R.id.ivClose)

        tvAddress.text = publicAddress

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(publicAddress, BarcodeFormat.QR_CODE, 400, 400)
        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                pixels[y * w + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        ivQR.setImageBitmap(bitmap)

        ivClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showSignTransactionDialog(isSuccess: Boolean, ethHash: String?) {
        val dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.popup_sign_transaction)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.animation
        val ivState = dialog.findViewById<AppCompatImageView>(R.id.ivState)
        val transactionState = dialog.findViewById<AppCompatTextView>(R.id.tvTransactionState)
        val transactionHash = dialog.findViewById<AppCompatTextView>(R.id.tvTransactionHash)
        val tvCopy = dialog.findViewById<AppCompatTextView>(R.id.tvCopy)
        val ivClose = dialog.findViewById<AppCompatImageView>(R.id.ivClose)

        if(isSuccess) {
            transactionHash.text = ethHash
            transactionState.text = getString(R.string.sign_success)
            ivState.setImageDrawable(getDrawable(R.drawable.ic_iv_transaction_success))
            tvCopy.setOnClickListener { copyToClipboard(transactionHash.text.toString()) }
        } else {
            transactionState.text = getString(R.string.sign_failed)
            ivState.setImageDrawable(getDrawable(R.drawable.ic_transaction_failed))
            transactionHash.hide()
            tvCopy.hide()
        }

        ivClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showTransactionDialog(transactionStatus: TransactionStatus) {
        val dialog = Dialog(this@MainActivity)
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

    private fun showConfirmTransactionDialog() {
        val dialog = Dialog(this@MainActivity)
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
        val btnCancel = dialog.findViewById<AppCompatButton>(R.id.btnCancel)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", text)
        clipboardManager.setPrimaryClip(clipData)
        toast("Text Copied")
    }

    private fun logout() {
        val logoutCompletableFuture =  web3Auth.logout()
        logoutCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong" )
            }
        }
    }
}