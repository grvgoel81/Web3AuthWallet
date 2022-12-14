package com.web3auth.wallet.utils

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
val Context.web3AuthWalletPreferences: SharedPreferences
    get() = EncryptedSharedPreferences.create(
        "Web3Auth",
        masterKeyAlias,
        this,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )


inline fun <reified T : Any> SharedPreferences.getObject(key: String): T? {
    return Gson().fromJson<T>(getString(key, null), T::class.java)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> SharedPreferences.get(key: String, defaultValue: T? = null): T {
    return when (T::class) {
        Boolean::class -> getBoolean(key, defaultValue as? Boolean? ?: false) as T
        Float::class -> getFloat(key, defaultValue as? Float? ?: 0.0f) as T
        Int::class -> getInt(key, defaultValue as? Int? ?: 0) as T
        Long::class -> getLong(key, defaultValue as? Long? ?: 0L) as T
        String::class -> getString(key, defaultValue as? String? ?: "") as T
        else -> {
            if (defaultValue is Set<*>) {
                getStringSet(key, defaultValue as Set<String>) as T
            } else {
                val typeName = T::class.java.simpleName
                throw Error("Unable to get shared preference with value type '$typeName'. Use getObject")
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
inline operator fun <reified T : Any> SharedPreferences.set(key: String, value: T) {
    with(edit()) {
        when (T::class) {
            Boolean::class -> putBoolean(key, value as Boolean)
            Float::class -> putFloat(key, value as Float)
            Int::class -> putInt(key, value as Int)
            Long::class -> putLong(key, value as Long)
            String::class -> putString(key, value as String)
            else -> {
                if (value is Set<*>) {
                    putStringSet(key, value as Set<String>)
                } else {
                    val json = Gson().toJson(value)
                    putString(key, json)
                }
            }
        }
        commit()
    }
}

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun Double.roundOff(): String = String.format("%.4f", this)

fun Long.roundOffLong(): BigDecimal =
    BigDecimal(this.div(10.0.pow(9))).setScale(2, RoundingMode.HALF_UP)

fun View.hide() {
    this.visibility = View.GONE
}

fun TextView.addLeftDrawable(drawable: Int, padding: Int = 12) {
    val imgDrawable = ContextCompat.getDrawable(context, drawable)
    compoundDrawablePadding = padding
    setCompoundDrawablesWithIntrinsicBounds(imgDrawable, null, null, null)
}

enum class TransactionStatus {
    PLACED,
    SUCCESSFUL,
    PENDING,
    FAILED
}

const val NETWORK = "network"
const val BLOCKCHAIN = "blockchain"
const val LOGIN_RESPONSE = "login-response"
const val SESSION_ID = "sessionId"
const val ED25519Key = "ed25519Key"
const val ISLOGGEDIN = "isLoggedIn"
const val ISONBOARDED = "isOnboarded"
const val PUBLICKEY = "publicKey"
const val LOGINTYPE = "loginType"
const val PRICE_IN_USD = "priceInUSD"
const val LOGOUT = "logout"
const val DATA = "data"
const val THEME = "theme"
const val LANGUAGE = "language"
const val IS_LANGUAGE_CHANGED = "isLanguageChanged"

