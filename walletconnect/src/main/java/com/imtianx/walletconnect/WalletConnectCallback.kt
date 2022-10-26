package com.imtianx.walletconnect

import com.imtianx.walletconnect.models.WCSignTransaction
import com.imtianx.walletconnect.models.ethereum.WCEthereumSignMessage
import com.imtianx.walletconnect.models.ethereum.WCEthereumTransaction
import com.imtianx.walletconnect.models.session.WCAddNetwork
import com.imtianx.walletconnect.models.session.WCRequestInfo

/**
 * <pre>
 *     @desc:
 * </pre>
 * @author imtianx
 * @date 2022/10/19 15:14
 */
interface WalletConnectCallback {

    fun onFailure(sessionUrl: String, error: Throwable) {}
    fun onDisconnect(sessionUrl: String, code: Int, reason: String) {}
    fun onSessionRequest(sessionUrl: String, id: Long, requestInfo: WCRequestInfo) {}

    fun onEthSign(sessionUrl: String, id: Long, message: WCEthereumSignMessage, chainId: String) {}
    fun onEthSignTransaction(
        sessionUrl: String,
        id: Long,
        transaction: WCEthereumTransaction,
        chainId: String
    ) {
    }

    fun onEthSendTransaction(
        sessionUrl: String,
        id: Long,
        transaction: WCEthereumTransaction,
        chainId: String
    ) {
    }

    fun onSignTransaction(
        sessionUrl: String,
        id: Long,
        transaction: WCSignTransaction,
        chainId: String
    ) {
    }

    fun onCustomRequest(sessionUrl: String, id: Long, payload: String) {}
    
    fun onGetAccounts(sessionUrl: String, id: Long) {}
    fun onWalletChangeNetwork(sessionUrl: String, id: Long, chainId: Int, chainName: String) {}
    fun onWalletAddNetwork(sessionUrl: String, id: Long, network: WCAddNetwork) {}
}