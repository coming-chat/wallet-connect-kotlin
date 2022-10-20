package com.imtianx.walletconnect.models.session

import com.imtianx.walletconnect.models.WCPeerMeta

data class WCApproveSessionResponse(
    val approved: Boolean = true,
    val chainId: Int,
    val accounts: List<String>,
    val peerId: String?,
    val peerMeta: WCPeerMeta?
)