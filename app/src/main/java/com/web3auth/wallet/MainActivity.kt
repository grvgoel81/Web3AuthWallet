package com.web3auth.wallet

import android.app.Dialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.os.postDelayed
import androidx.core.widget.addTextChangedListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.web3auth.core.Web3Auth
import com.web3auth.core.getCustomTabsBrowsers
import com.web3auth.core.getDefaultBrowser
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.WhiteLabelData
import com.web3auth.wallet.api.ApiHelper
import com.web3auth.wallet.api.Web3AuthApi
import com.web3auth.wallet.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.Web3ClientVersion
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var web3Auth: Web3Auth
    private var web3AuthResponse: Web3AuthResponse? = null
    private lateinit var web3: Web3j
    private lateinit var blockChain: String
    private lateinit var publicAddress: String
    private lateinit var web3Balance: EthGetBalance
    private lateinit var selectedNetwork: String
    private lateinit var tvExchangeRate: AppCompatTextView
    private lateinit var tvPriceInUSD: AppCompatTextView
    private lateinit var priceInUSD: String
    private lateinit var etMessage: AppCompatEditText
    private lateinit var btnSign: AppCompatButton
    private lateinit var tvBalance: AppCompatTextView
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        selectedNetwork =
            Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.getString(NETWORK, "Mainnet")
                .toString()
        showProgressDialog()
        if(!Web3AuthUtils.isNetworkAvailable(this@MainActivity)) {
            progressDialog.hide()
            longToast(getString(R.string.connect_to_internet))
            return
        }
        configureWeb3j()
        configureWeb3Auth()
        setData()
        setUpListeners()
    }

    private fun showProgressDialog() {
        progressDialog = ProgressDialog(this@MainActivity)
        progressDialog.setMessage(getString(R.string.loading_balance))
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun configureWeb3Auth() {
        Web3Auth(
            Web3AuthOptions(
                context = this,
                clientId = getString(R.string.web3auth_project_id),
                network = NetworkUtils.getWebAuthNetwork(selectedNetwork),
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
        web3AuthResponse =
            Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.getObject(LOGIN_RESPONSE)
        web3AuthResponse?.let { getEthAddress(it) }

        findViewById<AppCompatTextView>(R.id.tvName).text = "Welcome ".plus(
            web3AuthResponse?.userInfo?.name?.split(" ")?.get(0)
        ).plus("!")
        findViewById<AppCompatTextView>(R.id.tvEmail).text = web3AuthResponse?.userInfo?.email
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

    private fun setUpListeners() {
        tvExchangeRate = findViewById(R.id.tvExchangeRate)
        etMessage = findViewById(R.id.etMessage)
        tvPriceInUSD = findViewById(R.id.tvPriceInUSD)
        btnSign = findViewById(R.id.btnSign)
        tvBalance = findViewById(R.id.tvBalance)
        findViewById<AppCompatButton>(R.id.btnTransfer).setOnClickListener {
            startActivity(Intent(this@MainActivity, TransferAssetsActivity::class.java))
        }
        findViewById<AppCompatImageView>(R.id.ivLogout).setOnClickListener { logout() }
        findViewById<AppCompatTextView>(R.id.tvLogout).setOnClickListener { logout() }
        findViewById<AppCompatImageView>(R.id.ivQRCode).setOnClickListener {
            showQRDialog(publicAddress)
        }

        btnSign.setOnClickListener {
            if(etMessage.text.toString().isNullOrEmpty() || Web3AuthUtils.containsEmoji(etMessage.text.toString())) {
                toast("Invalid message")
                return@setOnClickListener
            }
            var signatureHash = getSignature(web3AuthResponse?.sessionId.toString(), etMessage.text.toString())
            if(signatureHash.isNullOrEmpty()) {
                showSignTransactionDialog(false)
            } else {
                showSignTransactionDialog(true, signatureHash)
            }
        }
    }

    private fun setData() {
        blockChain = Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.getString(BLOCKCHAIN, "Ethereum")
                .toString()
        findViewById<AppCompatTextView>(R.id.tvNetwork).text =
            blockChain.plus(" ").plus(selectedNetwork)

        findViewById<AppCompatTextView>(R.id.tvViewTransactionStatus).setOnClickListener {
            Web3AuthUtils.openCustomTabs(this@MainActivity,"https://mumbai.polygonscan.com/")
        }

        getCurrencyPriceInUSD(Web3AuthUtils.getCurrency(blockChain), "USD")

        if(blockChain == getString(R.string.solana)) {
            SolanaManager.createWallet(NetworkUtils.getSolanaNetwork(selectedNetwork))
        }
    }

    private fun getEthAddress(web3AuthResponse: Web3AuthResponse) {
        val authArgs = CustomAuthArgs(NetworkUtils.getTorusNetwork(selectedNetwork))
        authArgs.networkUrl =
            "https://small-long-brook.ropsten.quiknode.pro/e2fd2eb01412e80623787d1c40094465aa67624a"
        // Initialize CustomAuth
        var torusSdk = CustomAuth(authArgs)
        val verifier = web3AuthResponse.userInfo?.verifier
        val verifierId = web3AuthResponse.userInfo?.verifierId

        Executors.newSingleThreadExecutor().execute {
            publicAddress = torusSdk.getEthAddress(verifier, verifierId)
            runOnUiThread {
                findViewById<AppCompatTextView>(R.id.tvAddress).text =
                    publicAddress.take(3).plus("...").plus(publicAddress.takeLast(4))
                Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.set(PUBLICKEY, publicAddress)
                if(blockChain == getString(R.string.solana)) {
                    getSolanaBalance(publicAddress)
                } else {
                    retrieveBalance(publicAddress)
                }
            }
        }
    }

    private fun getCurrencyPriceInUSD(fsym: String, tsyms: String) {
        GlobalScope.launch {
            val web3AuthApi = ApiHelper.getTorusInstance().create(Web3AuthApi::class.java)
            val result = web3AuthApi.getCurrencyPrice(fsym, tsyms)
            if(result.isSuccessful && result.body() != null) {
                Handler(Looper.getMainLooper()).postDelayed(10) {
                    priceInUSD = result.body()?.USD.toString()
                    Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.set(PRICE_IN_USD, priceInUSD)
                    tvExchangeRate.text = "1 ".plus(fsym).plus(" = ").plus(priceInUSD).plus(" $tsyms")
                }
            }
        }
    }

    private fun retrieveBalance(publicAddress: String) {
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
                println("web3Balance: ${web3Balance.balance}" )
                tvBalance.text = Web3AuthUtils.toWeiEther(web3Balance).toString()
                var usdPrice = Web3AuthUtils.getPriceInUSD(web3Balance.balance.toDouble(), priceInUSD)
                tvPriceInUSD.text = "= ".plus(usdPrice).plus(" USD")
                progressDialog.hide()
            }
        }
    }

    private fun getSolanaBalance(publicAddress: String) {
        tvBalance.text = SolanaManager.getBalance(publicAddress).toString()
        var usdPrice = Web3AuthUtils.getPriceInUSD(SolanaManager.getBalance(publicAddress).toDouble(), priceInUSD)
        tvPriceInUSD.text = "= ".plus(usdPrice).plus(" USD")
        progressDialog.hide()
    }

    private fun getSignature(privateKey: String, message: String): String {
        val credentials: Credentials = Credentials.create(privateKey)
        val hashedData = Hash.sha3(message.toByteArray(StandardCharsets.UTF_8))
        val signature = Sign.signMessage(hashedData, credentials.ecKeyPair)
        val r = Numeric.toHexString(signature.r)
        val s = Numeric.toHexString(signature.s).substring(2)
        val v = Numeric.toHexString(signature.v).substring(2)
        return StringBuilder(r).append(s).append(v).toString()
    }

    private fun showQRDialog(publicAddress: String) {
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
        tvAddress.setOnClickListener { copyToClipboard(tvAddress.text.toString()) }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(publicAddress, BarcodeFormat.QR_CODE, 200, 200)
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

    private fun showSignTransactionDialog(isSuccess: Boolean, ethHash: String? = null) {
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

        if (isSuccess) {
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

    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", text)
        clipboardManager.setPrimaryClip(clipData)
        toast("Text Copied")
    }

    private fun logout() {
        val logoutCompletableFuture = web3Auth.logout()
        logoutCompletableFuture.whenComplete { _, error ->
            if (error == null) {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }
    }
}