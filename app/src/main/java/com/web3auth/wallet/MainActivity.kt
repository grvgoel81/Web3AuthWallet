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
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.web3auth.core.Web3Auth
import com.web3auth.core.types.Web3AuthOptions
import com.web3auth.core.types.Web3AuthResponse
import com.web3auth.core.types.WhiteLabelData
import com.web3auth.wallet.utils.*
import com.web3auth.wallet.viewmodel.EthereumViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var web3Auth: Web3Auth
    private var web3AuthResponse: Web3AuthResponse? = null
    private lateinit var blockChain: String
    private lateinit var publicAddress: String
    private lateinit var selectedNetwork: String
    private lateinit var tvExchangeRate: AppCompatTextView
    private lateinit var tvPriceInUSD: AppCompatTextView
    private var priceInUSD: String = ""
    private lateinit var etMessage: AppCompatEditText
    private lateinit var btnSign: AppCompatButton
    private lateinit var tvBalance: AppCompatTextView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var ethereumViewModel: EthereumViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        ethereumViewModel = ViewModelProvider(this)[EthereumViewModel::class.java]
        selectedNetwork =
            Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getString(NETWORK, "Mainnet")
                .toString()
        blockChain = Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getString(BLOCKCHAIN, "Ethereum")
            .toString()
        showProgressDialog()
        if(!Web3AuthUtils.isNetworkAvailable(this@MainActivity)) {
            progressDialog.dismiss()
            longToast(getString(R.string.connect_to_internet))
            return
        }
        setData()
        configureWeb3Auth()
        observeListeners()
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
            Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getObject(LOGIN_RESPONSE)

        ethereumViewModel.getPublicAddress(web3AuthResponse?.sessionId.toString())

        findViewById<AppCompatTextView>(R.id.tvName).text = getString(R.string.welcome).plus(" ").plus(
            web3AuthResponse?.userInfo?.name?.split(" ")?.get(0)
        ).plus("!")
        findViewById<AppCompatTextView>(R.id.tvEmail).text = web3AuthResponse?.userInfo?.email
        setData()
    }

    private fun observeListeners() {
        ethereumViewModel.isWeb3Configured.observe(this) {
            if (it == false) {
                toast("Error connecting to Web3j")
            }
        }

        ethereumViewModel.priceInUSD.observe(this) {
            if(!it.isNullOrEmpty()) {
                priceInUSD = it
                Web3AuthWalletApp.getContext().web3AuthWalletPreferences[PRICE_IN_USD] = priceInUSD
                tvExchangeRate.text = "1 ".plus(Web3AuthUtils.getCurrency(blockChain)).plus(" = ")
                    .plus(priceInUSD).plus(" " + getString(R.string.usd))
                ethereumViewModel.retrieveBalance(publicAddress)
            }
        }

        ethereumViewModel.publicAddress.observe(this) {
            publicAddress = it
            findViewById<AppCompatTextView>(R.id.tvAddress).text =
                publicAddress.take(3).plus("...").plus(publicAddress.takeLast(4))
            Web3AuthWalletApp.getContext().web3AuthWalletPreferences[PUBLICKEY] = publicAddress
            if(blockChain == getString(R.string.solana)) {
                getSolanaBalance(publicAddress)
            } else {
                if(publicAddress.isNotEmpty()) {
                    ethereumViewModel.retrieveBalance(publicAddress)
                }
            }
        }

        ethereumViewModel.balance.observe(this) {
            if (it > 0.0 && priceInUSD.isNotEmpty()) {
                    tvBalance.text = Web3AuthUtils.toWeiEther(it).roundOff()
                    val usdPrice = Web3AuthUtils.getPriceInUSD(it, priceInUSD.toDouble())
                    tvPriceInUSD.text = "= ".plus(usdPrice.toDouble().roundOff()).plus(" USD")
                    progressDialog.dismiss()
            }
        }
    }

    private fun setUpListeners() {
        tvExchangeRate = findViewById(R.id.tvExchangeRate)
        etMessage = findViewById(R.id.etMessage)
        tvPriceInUSD = findViewById(R.id.tvPriceInUSD)
        btnSign = findViewById(R.id.btnSign)
        tvBalance = findViewById(R.id.tvBalance)
        findViewById<AppCompatButton>(R.id.btnTransfer).setOnClickListener {
            val intent = Intent(this@MainActivity, TransferAssetsActivity::class.java)
            if(etMessage.text.toString().trim().isNotEmpty()) {
                intent.putExtra(DATA, etMessage.text.toString())
            }
            startActivity(intent)
        }
        findViewById<AppCompatImageView>(R.id.ivLogout).setOnClickListener { logout() }
        findViewById<AppCompatTextView>(R.id.tvLogout).setOnClickListener { logout() }
        findViewById<AppCompatImageView>(R.id.ivQRCode).setOnClickListener {
            showQRDialog(publicAddress)
        }

        btnSign.setOnClickListener {
            if(etMessage.text.toString().isNullOrEmpty() || Web3AuthUtils.containsEmoji(etMessage.text.toString())) {
                toast(getString(R.string.invalid_message))
                return@setOnClickListener
            }
            var signatureHash = ethereumViewModel.getSignature(web3AuthResponse?.sessionId.toString(), etMessage.text.toString())
            if(signatureHash.isEmpty()) {
                showSignTransactionDialog(false)
            } else {
                showSignTransactionDialog(true, signatureHash)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        web3Auth.setResultUrl(intent?.data)
    }

    private fun setData() {
        ethereumViewModel.getCurrencyPriceInUSD(Web3AuthUtils.getCurrency(blockChain), "USD")
        findViewById<AppCompatTextView>(R.id.tvNetwork).text =
            blockChain.plus(" ").plus(selectedNetwork)

        findViewById<AppCompatTextView>(R.id.tvViewTransactionStatus).setOnClickListener {
            Web3AuthUtils.openCustomTabs(this@MainActivity,"https://mumbai.polygonscan.com/")
        }

        if(blockChain == getString(R.string.solana)) {
            SolanaManager.createWallet(NetworkUtils.getSolanaNetwork(selectedNetwork))
        }
    }

    private fun getSolanaBalance(publicAddress: String) {
        tvBalance.text = SolanaManager.getBalance(publicAddress).toString()
        val usdPrice = Web3AuthUtils.getPriceInUSD(SolanaManager.getBalance(publicAddress).toDouble(), priceInUSD.toDouble())
        tvPriceInUSD.text = "= ".plus(usdPrice).plus(" USD")
        progressDialog.dismiss()
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

    override fun onRestart() {
        super.onRestart()
        ethereumViewModel.retrieveBalance(Web3AuthWalletApp.getContext().web3AuthWalletPreferences.get(PUBLICKEY, "").toString())
    }

    private fun logout() {
        Web3AuthWalletApp.getContext().web3AuthWalletPreferences[ISLOGGEDIN] = false
        Web3AuthWalletApp.getContext().web3AuthWalletPreferences[ISONBOARDED] = false
        Web3AuthWalletApp.getContext().web3AuthWalletPreferences[LOGOUT] = false
        Web3AuthWalletApp.getContext().web3AuthWalletPreferences.edit().clear().apply()
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}