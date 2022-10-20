package com.trustwallet.walletconnect

import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import okhttp3.OkHttpClient

/**
 * <pre>
 *     @desc: WCClient for more connections
 * </pre>
 * @author imtianx
 * @date 2022/10/19 18:20
 */
object WCClientManager {

    private val clientList = mutableMapOf<String, WCClient2>()

    fun connect(uri: String, peerMeta: WCPeerMeta, connectCallback: WalletConnectCallback? = null) {
        val session = WCSession.from(uri)
        if (session == null) {
            connectCallback?.onFailure(uri, Throwable("url is error!"))
            return
        }
        val client = WCClient2(GsonBuilder(), OkHttpClient()).apply {
            connectCallback?.let {
                addConnectListener(it)
            }
            clientList[uri] = this
        }
        clientList[session.toUri()] = client
        client.connect(session, peerMeta)
    }

    fun switchChain(uri: String, chainId: Int, chainName: String = "") {
        val client = clientList[uri]
        check(client != null) { "client not exist" }
        client.switchChain(chainId = chainId, chainName = chainName)
    }

    fun reConnect(
        uri: String,
        session: WCSessionStoreItem?,
        peerMeta: WCPeerMeta,
        connectCallback: WalletConnectCallback? = null
    ) {
        val client = if (clientList.containsKey(uri)) {
            clientList[uri]!!
        } else {
            WCClient2(GsonBuilder(), OkHttpClient()).apply {
                connectCallback?.let {
                    addConnectListener(it)
                }
                clientList[uri] = this
            }
        }
        connectCallback?.let {
            client.addConnectListener(it)
        }

        if (session == null) {
            client.connect(WCSession.from(uri)!!, peerMeta)
        } else {
            client.reConnectSession(session)
        }
    }

    fun removeReconnectCallback(uri: String, connectCallback: WalletConnectCallback?) {
        clientList[uri]?.let {
            if (connectCallback != null) {
                it.removeConnectListener(connectCallback)
            }
        }
    }

    fun approveSession(uri: String, accounts: List<String>, chainId: Int) {
        clientList[uri]?.approveSession(accounts, chainId)
    }

    fun rejectSession(uri: String) {
        clientList[uri]?.let {
            it.rejectSession()
            it.disconnect()
        }
    }

    fun killsession(uri: String) {
        clientList[uri]?.let {
            it.killSession()
            clientList.remove(uri)
        }
    }

    fun disconnect(uri: String) {
        val client = clientList[uri]
        if (client != null) {
            clientList.remove(uri)
            if (client.session != null) {
                client.killSession()
            } else {
                client.disconnect()
            }
        }
    }
}