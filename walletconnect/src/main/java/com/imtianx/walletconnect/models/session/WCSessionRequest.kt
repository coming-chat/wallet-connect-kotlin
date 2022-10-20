package com.imtianx.walletconnect.models.session

import com.imtianx.walletconnect.models.WCPeerMeta

data class WCSessionRequest(
    val peerId: String,
    val peerMeta: WCPeerMeta,
    val chainId: String?,
    val chain: String?
)