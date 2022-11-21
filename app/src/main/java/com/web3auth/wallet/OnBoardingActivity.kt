package com.web3auth.wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.web3auth.core.Web3Auth
import com.web3auth.core.isEmailValid
import com.web3auth.core.types.*
import com.web3auth.wallet.utils.*
import java8.util.concurrent.CompletableFuture


class OnBoardingActivity : AppCompatActivity() {

    private var expandFlag = false
    private lateinit var web3Auth: Web3Auth
    private lateinit var selectedNetwork: String
    private lateinit var ivFullLogin: AppCompatImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        supportActionBar?.hide()

        configureWeb3Auth()
        setUpListeners()
    }

    private fun setUpListeners() {
        val rlSocialLogins = findViewById<RelativeLayout>(R.id.rlSocialLogins)
        ivFullLogin = findViewById(R.id.ivFullLogin)
        ivFullLogin.setOnClickListener {
            expandFlag = if (expandFlag) {
                collapse(rlSocialLogins)
                false
            } else {
                expand(rlSocialLogins)
                true
            }
        }
        findViewById<AppCompatImageView>(R.id.ivBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<AppCompatButton>(R.id.btnContinue).setOnClickListener {
            signIn(
                Provider.EMAIL_PASSWORDLESS,
                ""
            )
        }
        findViewById<AppCompatImageView>(R.id.ivGoogle).setOnClickListener {
            signIn(
                Provider.GOOGLE,
                getString(R.string.google)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivFacebook).setOnClickListener {
            signIn(
                Provider.FACEBOOK,
                getString(R.string.facebook)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivTwitter).setOnClickListener {
            signIn(
                Provider.TWITTER,
                getString(R.string.twitter)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivDiscord).setOnClickListener {
            signIn(
                Provider.DISCORD,
                getString(R.string.discord)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivLine).setOnClickListener {
            signIn(
                Provider.LINE,
                getString(R.string.line)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivReddit).setOnClickListener {
            signIn(
                Provider.REDDIT,
                getString(R.string.reddit)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivApple).setOnClickListener {
            signIn(
                Provider.APPLE,
                getString(R.string.apple)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivLinkedin).setOnClickListener {
            signIn(
                Provider.LINKEDIN,
                getString(R.string.linkedin)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivWechat).setOnClickListener {
            signIn(
                Provider.WECHAT,
                getString(R.string.wechat)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivKakao).setOnClickListener {
            signIn(
                Provider.KAKAO,
                getString(R.string.kakao)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivGithub).setOnClickListener {
            signIn(
                Provider.GITHUB,
                getString(R.string.github)
            )
        }
        findViewById<AppCompatImageView>(R.id.ivTwitch).setOnClickListener {
            signIn(
                Provider.TWITCH,
                getString(R.string.twitch)
            )
        }
    }

    private fun configureWeb3Auth() {
        selectedNetwork =
            this.applicationContext.web3AuthWalletPreferences.getString(NETWORK, "")
                .toString()
        web3Auth = Web3Auth(
            Web3AuthOptions(
                context = this,
                clientId = getString(R.string.web3auth_project_id),
                network = NetworkUtils.getWebAuthNetwork(selectedNetwork),
                redirectUrl = Uri.parse(getString(R.string.web3Auth_redirection_url)),
                whiteLabel = WhiteLabelData(
                    getString(R.string.web3Auth_app_name), null, null, "en", true,
                    hashMapOf(
                        "primary" to "#123456"
                    )
                )
            )
        )

        web3Auth.setResultUrl(intent.data)

        val sessionResponse: CompletableFuture<Web3AuthResponse> = web3Auth.sessionResponse()
        print("SESSION MANAGEMENT");
        print(sessionResponse);
        sessionResponse.whenComplete { loginResponse, error ->
            if (error == null) {
                print("LOGIN RESPONSE $loginResponse")
                this.applicationContext.web3AuthWalletPreferences[LOGIN_RESPONSE] =
                    loginResponse
                this.applicationContext.web3AuthWalletPreferences[ED25519Key] =
                    loginResponse.ed25519PrivKey.toString()
                this.applicationContext.web3AuthWalletPreferences[ISONBOARDED] = true
                startActivity(Intent(this@OnBoardingActivity, MainActivity::class.java))
                finish()
            } else {
                Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
            }
        }
    }

    private fun signIn(loginProvider: Provider = Provider.GOOGLE, loginType: String) {
        this.applicationContext.web3AuthWalletPreferences[loginType] = loginType
        val hintEmailEditText = findViewById<AppCompatEditText>(R.id.etEmail)
        var extraLoginOptions: ExtraLoginOptions? = null
        if (loginProvider == Provider.EMAIL_PASSWORDLESS) {
            val hintEmail = hintEmailEditText.text.toString()
            if (hintEmail.isBlank() || !hintEmail.isEmailValid()) {
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_LONG).show()
                return
            }
            extraLoginOptions = ExtraLoginOptions(login_hint = hintEmail)
        }

        val loginCompletableFuture: CompletableFuture<Web3AuthResponse> = web3Auth.login(
            LoginParams(loginProvider, extraLoginOptions = extraLoginOptions)
        )
        loginCompletableFuture.whenComplete { loginResponse, error ->
            if (error == null) {
                println("LOGIN RESPONSE $loginResponse")
                this.applicationContext.web3AuthWalletPreferences[LOGIN_RESPONSE] =
                    loginResponse
                this.applicationContext.web3AuthWalletPreferences[ED25519Key] =
                    loginResponse.ed25519PrivKey.toString()
                this.applicationContext.web3AuthWalletPreferences[SESSION_ID] =
                    loginResponse.sessionId.toString()
                this.applicationContext.web3AuthWalletPreferences[ISONBOARDED] = true
                startActivity(Intent(this@OnBoardingActivity, MainActivity::class.java))
                finish()
            } else {
                Log.d(
                    getString(R.string.onboarding_error),
                    error.message ?: getString(R.string.something_went_wrong)
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle user signing in when app is active
        web3Auth.setResultUrl(intent?.data)
    }

    private fun expand(v: View) {
        v.measure(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        val targetHeight: Int = v.measuredHeight

        v.layoutParams.height = 1
        v.visibility = View.VISIBLE

        val a: Animation = object : Animation() {
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
        ivFullLogin.setImageDrawable(getDrawable(R.drawable.ic_collapse_arrow))
    }

    private fun collapse(v: View) {
        val initialHeight: Int = v.measuredHeight
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        a.duration = (initialHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
        ivFullLogin.setImageDrawable(getDrawable(R.drawable.ic_expand_arrow))
    }
}