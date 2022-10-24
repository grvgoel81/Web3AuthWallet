package com.web3auth.wallet

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import com.web3auth.core.Web3Auth
import com.web3auth.core.isEmailValid
import com.web3auth.core.types.*
import com.web3auth.wallet.utils.*
import java8.util.concurrent.CompletableFuture


class OnBoardingActivity: AppCompatActivity() {

    private var expandFlag = false
    private lateinit var web3Auth: Web3Auth
    private lateinit var selectedNetwork: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        supportActionBar?.hide()

        setUpListeners()
        configureWeb3Auth()
    }

    private fun setUpListeners() {
        val rlSocialLogins = findViewById<RelativeLayout>(R.id.rlSocialLogins)
        findViewById<AppCompatImageView>(R.id.ivFullLogin).setOnClickListener {
            expandFlag = if(expandFlag) {
                collapse(rlSocialLogins)
                false
            }  else {
                expand(rlSocialLogins)
                true
            }
        }
        findViewById<AppCompatImageView>(R.id.ivBack).setOnClickListener { onBackPressed() }
        findViewById<AppCompatButton>(R.id.btnContinue).setOnClickListener { signIn(Provider.EMAIL_PASSWORDLESS) }
        findViewById<AppCompatImageView>(R.id.ivGoogle).setOnClickListener { signIn(Provider.GOOGLE) }
        findViewById<AppCompatImageView>(R.id.ivFacebook).setOnClickListener { signIn(Provider.FACEBOOK) }
        findViewById<AppCompatImageView>(R.id.ivTwitter).setOnClickListener { signIn(Provider.TWITTER) }
        findViewById<AppCompatImageView>(R.id.ivDiscord).setOnClickListener { signIn(Provider.DISCORD) }
        findViewById<AppCompatImageView>(R.id.ivLine).setOnClickListener { signIn(Provider.LINE) }
        findViewById<AppCompatImageView>(R.id.ivReddit).setOnClickListener { signIn(Provider.REDDIT) }
        findViewById<AppCompatImageView>(R.id.ivApple).setOnClickListener { signIn(Provider.APPLE) }
        findViewById<AppCompatImageView>(R.id.ivLinkedin).setOnClickListener { signIn(Provider.LINKEDIN) }
        findViewById<AppCompatImageView>(R.id.ivWechat).setOnClickListener { signIn(Provider.WECHAT) }
        findViewById<AppCompatImageView>(R.id.ivKakao).setOnClickListener { signIn(Provider.KAKAO) }
        findViewById<AppCompatImageView>(R.id.ivGithub).setOnClickListener { signIn(Provider.GITHUB) }
        findViewById<AppCompatImageView>(R.id.ivTwitter).setOnClickListener { signIn(Provider.TWITCH) }
    }

    private fun configureWeb3Auth() {
        selectedNetwork = Web3AuthApp.getContext()?.web3AuthWalletPreferences?.getString(NETWORK, "").toString()
        web3Auth = Web3Auth(
            Web3AuthOptions(context = this,
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
        )

        web3Auth.setResultUrl(intent.data)
    }

    private fun signIn(loginProvider: Provider = Provider.GOOGLE) {
        val hintEmailEditText = findViewById<AppCompatEditText>(R.id.etEmail)
        var extraLoginOptions: ExtraLoginOptions? = null
        if (loginProvider == Provider.EMAIL_PASSWORDLESS) {
            val hintEmail = hintEmailEditText.text.toString()
            if (hintEmail.isBlank() || !hintEmail.isEmailValid()) {
                Toast.makeText(this, "Please enter a valid Email.", Toast.LENGTH_LONG).show()
                return
            }
            extraLoginOptions = ExtraLoginOptions(login_hint = hintEmail)
        }

        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.login(
            LoginParams(loginProvider, extraLoginOptions = extraLoginOptions)
        )
        loginCompletableFuture.whenComplete { loginResponse, error ->
            if (error == null) {
                Web3AuthApp.getContext()?.web3AuthWalletPreferences?.set(LOGIN_RESPONSE, loginResponse)
                startActivity(Intent(this@OnBoardingActivity, MainActivity::class.java))
                Web3AuthApp.getContext()?.web3AuthWalletPreferences?.set(ISONBOARDED, true)
            } else {
                Log.d("OnBoardingError", error.message ?: "Something went wrong" )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle user signing in when app is active
        web3Auth.setResultUrl(intent?.data)
    }

    private fun expand(v: View){
        v.measure(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT)
        val targetHeight: Int = v.measuredHeight

        v.layoutParams.height = 1
        v.visibility = View.VISIBLE

        val a: Animation = object : Animation(){
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                v.layoutParams.height = if (interpolatedTime == 1f)
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                else
                    (targetHeight * interpolatedTime).toInt()
                v.requestLayout()

            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }

    private fun collapse(v: View) {
        val initialHeight : Int = v.measuredHeight
        val a : Animation = object : Animation(){
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (interpolatedTime == 1f){
                    v.visibility = View.GONE
                }else{
                    v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        a.duration = (initialHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }
}