package com.web3auth.wallet

import android.app.Application
import android.content.Context

open class Web3AuthWalletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        mInstance = this
    }

    companion object {
        lateinit var mInstance: Web3AuthWalletApp
        fun getContext(): Context? {
            return mInstance.applicationContext
        }
    }
}