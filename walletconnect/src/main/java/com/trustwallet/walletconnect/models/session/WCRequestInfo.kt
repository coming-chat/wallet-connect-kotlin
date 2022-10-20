package com.trustwallet.walletconnect.models.session

import com.trustwallet.walletconnect.WCSessionStoreItem

/**
 * <pre>
 *     @desc:
 * </pre>
 * @author imtianx
 * @date 2022/10/20 11:39
 */
data class WCRequestInfo(
    val chainId: String = "0",
    val chainName: String = "",
    val sessionStore: WCSessionStoreItem
)