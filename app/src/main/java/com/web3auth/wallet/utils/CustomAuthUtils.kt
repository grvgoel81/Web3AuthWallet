package com.web3auth.wallet.utils

import org.torusresearch.fetchnodedetails.FetchNodeDetails
import org.torusresearch.torusutils.TorusUtils
import org.torusresearch.torusutils.helpers.Utils
import org.torusresearch.torusutils.types.TorusCtorOptions
import org.torusresearch.torusutils.types.VerifierArgs
import java.util.concurrent.ExecutionException

class CustomAuth(_customAuthArgs: CustomAuthArgs) {
    private var nodeDetailManager: FetchNodeDetails? = null
    private val torusUtils: TorusUtils
    private var customAuthArgs: CustomAuthArgs

    init {
        customAuthArgs = _customAuthArgs
        nodeDetailManager = if (Utils.isEmpty(_customAuthArgs.networkUrl)) {
            FetchNodeDetails(
                _customAuthArgs.network,
                CustomAuthArgs.CONTRACT_MAP[_customAuthArgs.network]
            )
        } else {
            FetchNodeDetails(
                _customAuthArgs.networkUrl,
                CustomAuthArgs.CONTRACT_MAP[_customAuthArgs.network]
            )
        }
        val opts = TorusCtorOptions("Web3Auth")
        opts.isEnableOneKey = true
        opts.network = _customAuthArgs.network.toString()
        opts.signerHost =
            CustomAuthArgs.SIGNER_MAP[_customAuthArgs.network].toString() + "/api/sign"
        opts.allowHost =
            CustomAuthArgs.SIGNER_MAP[_customAuthArgs.network].toString() + "/api/allow"
        torusUtils = TorusUtils(opts)
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    fun getEthAddress(verifier: String?, verifierId: String?)
    : String {
        val nodeDetails = nodeDetailManager?.getNodeDetails(verifier, verifierId)?.get()
        // this function creates a wallet if not doesn't exist
        return torusUtils.getPublicAddress(nodeDetails?.torusNodeEndpoints, nodeDetails?.torusNodePub,
            VerifierArgs(verifier, verifierId)).get().address
    }
}