package com.web3auth.wallet

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.web3auth.wallet.utils.*


class SplashActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()
        Handler(Looper.getMainLooper()).postDelayed({ navigate() }, 500)
    }

    private fun navigate() {
        val loggedInFlag = Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.get(ISLOGGEDIN, false)
        val onboardedFlag = Web3AuthWalletApp.getContext()?.web3AuthWalletPreferences?.get(ISONBOARDED, false)
        var intent = Intent(this@SplashActivity, LoginActivity::class.java)
        if(onboardedFlag == true) {
             intent = Intent(this@SplashActivity, MainActivity::class.java)
        } else if(loggedInFlag == true) {
             intent = Intent(this@SplashActivity, OnBoardingActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}