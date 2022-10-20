package com.imtianx.walletconnect.models.session

import com.google.gson.annotations.SerializedName

data class WCChangeNetwork(
    @SerializedName("chainId")
    val chainIdHex: String,
    @SerializedName("chain")
    val chainName: String? = ""
)