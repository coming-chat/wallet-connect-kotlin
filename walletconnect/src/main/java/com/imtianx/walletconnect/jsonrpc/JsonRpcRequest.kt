package com.imtianx.walletconnect.jsonrpc

import com.imtianx.walletconnect.JSONRPC_VERSION
import com.imtianx.walletconnect.models.WCMethod

data class JsonRpcRequest<T>(
    val id: Long,
    val jsonrpc: String = JSONRPC_VERSION,
    val method: WCMethod?,
    val params: T
)