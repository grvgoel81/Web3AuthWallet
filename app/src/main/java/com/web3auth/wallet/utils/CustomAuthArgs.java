package com.web3auth.wallet.utils;

import org.torusresearch.fetchnodedetails.FetchNodeDetails;
import org.torusresearch.fetchnodedetails.types.TorusNetwork;

import java.util.HashMap;

public class CustomAuthArgs {

    public static HashMap<TorusNetwork, String> CONTRACT_MAP = new HashMap<TorusNetwork, String>() {{
        put(TorusNetwork.MAINNET, FetchNodeDetails.PROXY_ADDRESS_MAINNET);
        put(TorusNetwork.TESTNET, FetchNodeDetails.PROXY_ADDRESS_TESTNET);
        put(TorusNetwork.CYAN, FetchNodeDetails.PROXY_ADDRESS_CYAN);
        put(TorusNetwork.AQUA, FetchNodeDetails.PROXY_ADDRESS_AQUA);
    }};

    public static HashMap<TorusNetwork, String> SIGNER_MAP = new HashMap<TorusNetwork, String>() {{
        put(TorusNetwork.MAINNET, "https://signer.tor.us");
        put(TorusNetwork.TESTNET, "https://signer.tor.us");
        put(TorusNetwork.CYAN, "https://signer-polygon.tor.us");
        put(TorusNetwork.AQUA, "https://signer-polygon.tor.us");
    }};


    // Android package redirect uri
    private TorusNetwork network;
    private boolean enableOneKey;
    private String networkUrl;


    public CustomAuthArgs(TorusNetwork network) {
        this.network = network;
    }


    public TorusNetwork getNetwork() {
        return network;
    }

    public void setNetwork(TorusNetwork network) {
        this.network = network;
    }

    public boolean isEnableOneKey() {
        return enableOneKey;
    }

    public void setEnableOneKey(boolean enableOneKey) {
        this.enableOneKey = enableOneKey;
    }

    public String getNetworkUrl() {
        return networkUrl;
    }

    public void setNetworkUrl(String networkUrl) {
        this.networkUrl = networkUrl;
    }
}
