package com.web3auth.wallet.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.web3auth.core.getCustomTabsBrowsers
import com.web3auth.core.getDefaultBrowser
import org.web3j.crypto.ECKeyPair
import org.web3j.protocol.core.methods.response.EthGetBalance
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import kotlin.math.pow

object Web3AuthUtils {

    fun getBlockChainName(blockChain: String): String {
        return when (blockChain) {
            "Ethereum" -> "EthAddress"
            "Solana" -> "SolAddress"
            else -> "EthAddress"
        }
    }

    fun getCurrency(blockChain: String): String {
        return when (blockChain) {
            "Ethereum" -> "ETH"
            "Solana" -> "SOL"
            else -> "EthAddress"
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return true
                }
            }
        }
        return false
    }

    fun getEtherInWei() = 10.0.pow(18)

    private fun getEtherInGwei() = 10.0.pow(9)

    fun toWeiEther(ethBalance: EthGetBalance): Double {
        var decimalWei = ethBalance.balance.toDouble()
        return decimalWei / getEtherInWei()
    }

    fun toGwieEther(balance: BigDecimal): Double {
        var decimalWei = balance.toDouble()
        return decimalWei / getEtherInGwei()
    }

    fun getPrivateKey(sessionId: String): String {
        val derivedECKeyPair: ECKeyPair = ECKeyPair.create(BigInteger(sessionId, 16))
        return derivedECKeyPair.privateKey.toString(16)
    }

    fun isValidEthAddress(address: String): Boolean {
        val ethAddressRegex = Regex(pattern = "^0x[a-fA-F0-9]{40}$")
        return ethAddressRegex.matches(input = address)
    }

    fun convertMinsToSec(mins: Double): Double = mins * 60L

    fun getMaxTransactionFee(amount: Double): Double =
        toGwieEther(BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(21000)))

    fun containsEmoji(message: String): Boolean {
        val emojiRegex = Regex(pattern = "(?:[\uD83C\uDF00-\uD83D\uDDFF]|[\uD83E\uDD00-\uD83E\uDDFF]|" +
                "[\uD83D\uDE00-\uD83D\uDE4F]|[\uD83D\uDE80-\uD83D\uDEFF]|" +
                "[\u2600-\u26FF]\uFE0F?|[\u2700-\u27BF]\uFE0F?|\u24C2\uFE0F?|" +
                "[\uD83C\uDDE6-\uD83C\uDDFF]{1,2}|" +
                "[\uD83C\uDD70\uD83C\uDD71\uD83C\uDD7E\uD83C\uDD7F\uD83C\uDD8E\uD83C\uDD91-\uD83C\uDD9A]\uFE0F?|" +
                "[\u0023\u002A\u0030-\u0039]\uFE0F?\u20E3|[\u2194-\u2199\u21A9-\u21AA]\uFE0F?|[\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55]\uFE0F?|" +
                "[\u2934\u2935]\uFE0F?|[\u3030\u303D]\uFE0F?|[\u3297\u3299]\uFE0F?|" +
                "[\uD83C\uDE01\uD83C\uDE02\uD83C\uDE1A\uD83C\uDE2F\uD83C\uDE32-\uD83C\uDE3A\uD83C\uDE50\uD83C\uDE51]\uFE0F?|" +
                "[\u203C\u2049]\uFE0F?|[\u25AA\u25AB\u25B6\u25C0\u25FB-\u25FE]\uFE0F?|" +
                "[\u00A9\u00AE]\uFE0F?|[\u2122\u2139]\uFE0F?|\uD83C\uDC04\uFE0F?|\uD83C\uDCCF\uFE0F?|" +
                "[\u231A\u231B\u2328\u23CF\u23E9-\u23F3\u23F8-\u23FA]\uFE0F?)+")
        return  emojiRegex.containsMatchIn(input = message)
    }

    fun getPriceInUSD(balance: Double, priceInUSD:String): BigDecimal =
        BigDecimal(balance).multiply(BigDecimal(priceInUSD))/getEtherInWei().toBigDecimal()

    fun getPriceinUSD(ethAmount: Double, usdPrice: Double): Double = ethAmount * (usdPrice)

    fun getPriceInEth(amount: Double, usdPrice: Double) = amount/usdPrice

    fun openCustomTabs(context: Context, url: String) {
        val defaultBrowser = context.getDefaultBrowser()
        val customTabsBrowsers = context.getCustomTabsBrowsers()

        if (customTabsBrowsers.contains(defaultBrowser)) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(defaultBrowser)
            customTabs.launchUrl(context, Uri.parse(url))
        } else if (customTabsBrowsers.isNotEmpty()) {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.intent.setPackage(customTabsBrowsers[0])
            customTabs.launchUrl(context, Uri.parse(url))
        }
    }
}