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
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Spinner
import androidx.annotation.RequiresApi
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
import com.web3auth.wallet.viewmodel.SolanaViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var web3Auth: Web3Auth
    private var web3AuthResponse: Web3AuthResponse? = null
    private lateinit var blockChain: String
    private lateinit var publicAddress: String
    private lateinit var selectedNetwork: String
    private lateinit var ed25519key: String
    private lateinit var tvExchangeRate: AppCompatTextView
    private lateinit var tvViewTransactionStatus: AppCompatTextView
    private lateinit var spCurrency: Spinner
    private var priceInUSD: String = ""
    private lateinit var etMessage: AppCompatEditText
    private lateinit var btnSign: AppCompatButton
    private lateinit var tvBalance: AppCompatTextView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var ethereumViewModel: EthereumViewModel
    private lateinit var solanaViewModel: SolanaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        selectedNetwork =
            Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getString(NETWORK, "Mainnet")
                .toString()
        blockChain = Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getString(BLOCKCHAIN, "ETH Mainnet")
            .toString()
        web3AuthResponse =
            Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getObject(LOGIN_RESPONSE)
        ed25519key = Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getString(ED25519Key, "").toString()

        showProgressDialog()

        if(blockChain.contains(getString(R.string.solana))) {
            solanaViewModel = ViewModelProvider(this)[SolanaViewModel::class.java]
            solanaViewModel.setNetwork(NetworkUtils.getSolanaNetwork(blockChain), ed25519key)
            solanaViewModel.getCurrencyPriceInUSD(Web3AuthUtils.getCurrency(blockChain), "USD")
            solanaViewModel.getPublicAddress()
        } else {
            ethereumViewModel = ViewModelProvider(this)[EthereumViewModel::class.java]
        }

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

        if(!blockChain.contains(getString(R.string.solana))) {
            ethereumViewModel.getPublicAddress(web3AuthResponse?.sessionId.toString())
        }

        findViewById<AppCompatTextView>(R.id.tvName).text = getString(R.string.welcome).plus(" ").plus(
            web3AuthResponse?.userInfo?.name?.split(" ")?.get(0)
        ).plus("!")
        
        val tvEmail = findViewById<AppCompatTextView>(R.id.tvEmail)
        tvEmail.text = web3AuthResponse?.userInfo?.email
        val loginType = Web3AuthWalletApp.getContext().web3AuthWalletPreferences.getString(LOGINTYPE, "")
        tvEmail.addLeftDrawable(Web3AuthUtils.getSocialLoginIcon(this, loginType.toString()))
        setData()
    }

    private fun observeListeners() {
        if (!blockChain.contains(getString(R.string.solana))) {
            ethereumViewModel.isWeb3Configured.observe(this) {
                if (it == false) {
                    toast("Error connecting to Web3j")
                }
            }

            ethereumViewModel.priceInUSD.observe(this) {
                if (!it.isNullOrEmpty()) {
                    priceInUSD = it
                    Web3AuthWalletApp.getContext().web3AuthWalletPreferences[PRICE_IN_USD] = priceInUSD
                    tvExchangeRate.text =
                        "1 ".plus(Web3AuthUtils.getCurrency(blockChain)).plus(" = ")
                            .plus(priceInUSD).plus(" " + getString(R.string.usd))
                    if (publicAddress.isNotEmpty()) {
                        ethereumViewModel.retrieveBalance(publicAddress)
                    }
                }
            }

            ethereumViewModel.publicAddress.observe(this) {
                publicAddress = it
                findViewById<AppCompatTextView>(R.id.tvAddress).text =
                    publicAddress.take(3).plus("...").plus(publicAddress.takeLast(4))
                Web3AuthWalletApp.getContext().web3AuthWalletPreferences[PUBLICKEY] = publicAddress
                if (publicAddress.isNotEmpty()) {
                    ethereumViewModel.retrieveBalance(publicAddress)
                }
            }

            ethereumViewModel.balance.observe(this) { it ->
                progressDialog.dismiss()
                if (it > 0.0 && priceInUSD.isNotEmpty()) {
                    tvBalance.text = Web3AuthUtils.toWeiEther(it).roundOff()
                }
            }
        } else {
            solanaViewModel.priceInUSD.observe(this) {
                progressDialog.dismiss()
                if (it != null && it.isNotEmpty()) {
                    priceInUSD = it
                    Web3AuthWalletApp.getContext().web3AuthWalletPreferences[PRICE_IN_USD] = priceInUSD
                    tvExchangeRate.text =
                        "1 ".plus(Web3AuthUtils.getCurrency(blockChain)).plus(" = ")
                            .plus(priceInUSD).plus(" " + getString(R.string.usd))
                }
            }
            solanaViewModel.publicAddress.observe(this) {
                if(it.isNotEmpty()) {
                    publicAddress = it
                    findViewById<AppCompatTextView>(R.id.tvAddress).text =
                        publicAddress.take(3).plus("...").plus(publicAddress.takeLast(4))
                    Web3AuthWalletApp.getContext().web3AuthWalletPreferences[PUBLICKEY] = publicAddress
                    solanaViewModel.getBalance(publicAddress)
                }
            }
            solanaViewModel.privateKey.observe(this) {
                println("Private Key: $it")
            }
            solanaViewModel.balance.observe(this) {
                if(it > 0) {
                    tvBalance.text = String.format("%.4f", it.roundOffLong())
                }
            }
            solanaViewModel.signature.observe(this) {
                if(it.isNotEmpty()) {
                    showSignatureResult(it)
                }
            }
        }
    }

    private fun setUpListeners() {
        tvExchangeRate = findViewById(R.id.tvExchangeRate)
        etMessage = findViewById(R.id.etMessage)
        btnSign = findViewById(R.id.btnSign)
        tvBalance = findViewById(R.id.tvBalance)
        findViewById<AppCompatButton>(R.id.btnTransfer).setOnClickListener {
            if(tvBalance.text.toString().toDouble().compareTo(0.0) == 0)  {
                toast(getString(R.string.insufficient_balance))
                return@setOnClickListener
            }
            val intent = Intent(this@MainActivity, TransferAssetsActivity::class.java)
            if(etMessage.text.toString().trim().isNotEmpty()) {
                intent.putExtra(DATA, etMessage.text.toString())
            }
            startActivity(intent)
        }
        var ivLogout = findViewById<AppCompatImageView>(R.id.ivLogout)
        ivLogout.setOnClickListener { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showPopupOption(ivLogout)
        }
        }
        findViewById<AppCompatTextView>(R.id.tvLogout).setOnClickListener { logout() }
        findViewById<AppCompatImageView>(R.id.ivQRCode).setOnClickListener {
            showQRDialog(publicAddress)
        }

        btnSign.setOnClickListener {
            val msg = etMessage.text.toString()
            if(msg.isEmpty() || Web3AuthUtils.containsEmoji(etMessage.text.toString())) {
                toast(getString(R.string.invalid_message))
                return@setOnClickListener
            }
            
            if(blockChain.contains(getString(R.string.solana))) {
                solanaViewModel.signTransaction(NetworkUtils.getSolanaNetwork(blockChain), msg)
            } else {
                val signatureHash = ethereumViewModel.getSignature(
                    web3AuthResponse?.sessionId.toString(),
                    msg)
                showSignatureResult(signatureHash)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        web3Auth.setResultUrl(intent?.data)
    }

    private fun setData() {
        findViewById<AppCompatTextView>(R.id.tvNetwork).text = blockChain.plus(" ")
        spCurrency = findViewById(R.id.spCurrency)
        setUpSpinner()

        tvViewTransactionStatus = findViewById(R.id.tvViewTransactionStatus)
        tvViewTransactionStatus.text = Web3AuthUtils.getTransactionStatusText(this, blockChain)
        tvViewTransactionStatus.setOnClickListener {
            Web3AuthUtils.openCustomTabs(this@MainActivity, Web3AuthUtils.getViewTransactionUrl(this, blockChain))
        }

        if(!blockChain.contains(getString(R.string.solana))) {
            ethereumViewModel.getCurrencyPriceInUSD(Web3AuthUtils.getCurrency(blockChain), "USD")
        }
    }

    private fun setUpSpinner() {
        var currencies: MutableList<String> = mutableListOf()
        currencies.add(Web3AuthUtils.getCurrency(blockChain))
        currencies.add(getString(R.string.usd))
        val currencyAdapter: ArrayAdapter<String> =
            ArrayAdapter(this, R.layout.item_dropdown, currencies)
        spCurrency.adapter = currencyAdapter
        spCurrency.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if(currencies[position] == getString(R.string.usd)) {
                    getCurrencyInUSD()
                } else {
                    getCurrencyInSelectedBlockChain()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
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

    private fun getCurrencyInUSD() {
        if(blockChain.contains(getString(R.string.solana))) {
            tvBalance.text = String.format("%.4f", Web3AuthUtils.getPriceinUSD(tvBalance.text.toString().toDouble(), priceInUSD.toDouble()))
        } else {
            tvBalance.text = String.format("%.6f", Web3AuthUtils.getPriceinUSD(tvBalance.text.toString().toDouble(), priceInUSD.toDouble()))
        }
    }
    
    private fun showSignatureResult(signatureHash: String) {
        if(signatureHash == "error") {
            showSignTransactionDialog(false)
        } else {
            showSignTransactionDialog(true, signatureHash)
        }
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

    private fun getCurrencyInSelectedBlockChain() {
        if(blockChain.contains(getString(R.string.solana))) {
            solanaViewModel.getBalance(publicAddress)
        } else {
            ethereumViewModel.retrieveBalance(publicAddress)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", text)
        clipboardManager.setPrimaryClip(clipData)
        toast("Text Copied")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        /*if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }*/
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPopupOption(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.main,popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.logout -> {
                    logout()
                }
            }
            true
        }
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }

    override fun onRestart() {
        super.onRestart()
        getCurrencyInSelectedBlockChain()
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