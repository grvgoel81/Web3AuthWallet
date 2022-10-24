package com.web3auth.wallet

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions


class TransferAssetsActivity: AppCompatActivity() {

    private lateinit var etBlockchainAddress: AppCompatEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_asset)
        supportActionBar?.hide()
        setUpListeners()
    }

    private fun setUpListeners() {
        etBlockchainAddress = findViewById(R.id.etBlockchainAddress)
        findViewById<AppCompatImageView>(R.id.ivScan).setOnClickListener { scanQRCode() }
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
            etBlockchainAddress.setText(result.contents)
        }
    }
}