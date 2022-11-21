package com.web3auth.wallet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.web3auth.wallet.utils.ISLOGGEDIN
import com.web3auth.wallet.utils.ISONBOARDED
import com.web3auth.wallet.utils.get
import com.web3auth.wallet.utils.web3AuthWalletPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()
        GlobalScope.launch {
            delay(500)
            navigate()
        }
    }

    private fun navigate() {
        val loggedInFlag =
            this.applicationContext.web3AuthWalletPreferences.get(ISLOGGEDIN, false)
        val onboardedFlag =
            this.applicationContext.web3AuthWalletPreferences.get(ISONBOARDED, false)
        var intent = Intent(this@SplashActivity, LoginActivity::class.java)
        if (onboardedFlag) {
            intent = Intent(this@SplashActivity, MainActivity::class.java)
        } else if (loggedInFlag) {
            intent = Intent(this@SplashActivity, OnBoardingActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}