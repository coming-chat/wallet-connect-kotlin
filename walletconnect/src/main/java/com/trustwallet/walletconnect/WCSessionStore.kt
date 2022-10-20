package com.trustwallet.walletconnect

import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import java.util.*

data class WCSessionStoreItem(
        val session: WCSession,
        var chainId: Int,
        val peerId: String,
        val remotePeerId: String,
        val remotePeerMeta: WCPeerMeta,
        val isAutoSign: Boolean = false,
        var date: Date = Date()
)

interface WCSessionStore {
    var session: WCSessionStoreItem?
}