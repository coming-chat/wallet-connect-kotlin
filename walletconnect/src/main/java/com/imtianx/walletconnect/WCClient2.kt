package com.imtianx.walletconnect

import android.util.Log
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.github.salomonbrys.kotson.typeToken
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.imtianx.walletconnect.exceptions.InvalidJsonRpcParamsException
import com.imtianx.walletconnect.extensions.hexStringToByteArray
import com.imtianx.walletconnect.jsonrpc.JsonRpcError
import com.imtianx.walletconnect.jsonrpc.JsonRpcErrorResponse
import com.imtianx.walletconnect.jsonrpc.JsonRpcRequest
import com.imtianx.walletconnect.jsonrpc.JsonRpcResponse
import com.imtianx.walletconnect.models.*
import com.imtianx.walletconnect.models.ethereum.WCEthereumSignMessage
import com.imtianx.walletconnect.models.ethereum.WCEthereumTransaction
import com.imtianx.walletconnect.models.ethereum.ethTransactionSerializer
import com.imtianx.walletconnect.models.session.*
import okhttp3.*
import okio.ByteString
import java.util.*

open class WCClient2(
    builder: GsonBuilder = GsonBuilder(),
    private val httpClient: OkHttpClient,
) : WebSocketListener(), WalletConnectCallback {

    private val TAG = "WCClient"

    private val gson = builder
        .serializeNulls()
        .registerTypeAdapter(ethTransactionSerializer)
        .create()

    private var socket: WebSocket? = null

    private val socketListeners: MutableSet<WebSocketListener> = mutableSetOf()
    private val connectListeners: MutableSet<WalletConnectCallback> = mutableSetOf()

    var session: WCSession? = null
        private set

    var peerMeta: WCPeerMeta? = null
        private set

    var peerId: String? = null
        private set

    var remotePeerId: String? = null
        private set

    var chainId: String? = null
        private set

    var isConnected: Boolean = false
        private set

    private var handshakeId: Long = -1

    private var sessionUrl = ""

    override fun onFailure(sessionUrl: String, error: Throwable) {
        connectListeners.forEach { it.onFailure(sessionUrl, error) }
    }

    override fun onDisconnect(sessionUrl: String, code: Int, reason: String) {
        connectListeners.forEach { it.onDisconnect(sessionUrl, code, reason) }
    }

    override fun onSessionRequest(sessionUrl: String, id: Long, requestInfo: WCRequestInfo) {
        connectListeners.forEach { it.onSessionRequest(sessionUrl, id, requestInfo) }
    }

    override fun onEthSign(sessionUrl: String, id: Long, message: WCEthereumSignMessage) {
        connectListeners.forEach { it.onEthSign(sessionUrl, id, message) }
    }

    override fun onEthSignTransaction(
        sessionUrl: String,
        id: Long,
        transaction: WCEthereumTransaction
    ) {
        connectListeners.forEach { it.onEthSignTransaction(sessionUrl, id, transaction) }
    }

    override fun onEthSendTransaction(
        sessionUrl: String,
        id: Long,
        transaction: WCEthereumTransaction
    ) {
        connectListeners.forEach { it.onEthSendTransaction(sessionUrl, id, transaction) }
    }

    override fun onCustomRequest(sessionUrl: String, id: Long, payload: String) {

    }

    override fun onGetAccounts(sessionUrl: String, id: Long) {
        connectListeners.forEach { it.onGetAccounts(sessionUrl, id) }
    }

    override fun onSignTransaction(sessionUrl: String, id: Long, transaction: WCSignTransaction) {
        connectListeners.forEach { it.onSignTransaction(sessionUrl, id, transaction) }
    }

    override fun onWalletChangeNetwork(
        sessionUrl: String,
        id: Long,
        chainId: Int,
        chainName: String
    ) {
        if (id > 0) {
            switchChain(id, chainId, chainName)
        }
        connectListeners.forEach { it.onWalletChangeNetwork(sessionUrl, id, chainId, chainName) }
    }

    override fun onWalletAddNetwork(sessionUrl: String, id: Long, network: WCAddNetwork) {
        connectListeners.forEach { it.onWalletAddNetwork(sessionUrl, id, network) }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d(TAG, "<< websocket opened >>")
        isConnected = true

        socketListeners.forEach { it.onOpen(webSocket, response) }

        val session =
            this.session ?: throw IllegalStateException("session can't be null on connection open")
        val peerId =
            this.peerId ?: throw IllegalStateException("peerId can't be null on connection open")
        // The Session.topic channel is used to listen session request messages only.
        subscribe(session.topic)
        // The peerId channel is used to listen to all messages sent to this httpClient.
        subscribe(peerId)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        var decrypted: String? = null
        try {
            Log.d(TAG, "<== message $text")
            decrypted = decryptMessage(text)
            Log.d(TAG, "<== decrypted $decrypted")
            handleMessage(decrypted)
        } catch (e: Exception) {
            onFailure(sessionUrl, e)
        } finally {
            socketListeners.forEach { it.onMessage(webSocket, decrypted ?: text) }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onFailure(sessionUrl, t)
        resetState()
        socketListeners.forEach { it.onFailure(webSocket, t, response) }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "<< websocket closed >>")
        socketListeners.forEach { it.onClosed(webSocket, code, reason) }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d(TAG, "<== pong")
        socketListeners.forEach { it.onMessage(webSocket, bytes) }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "<< closing socket >>")
        socketListeners.forEach { it.onClosing(webSocket, code, reason) }
        onDisconnect(sessionUrl, code, reason)
        resetState()
    }

    fun connect(
        session: WCSession,
        peerMeta: WCPeerMeta,
        peerId: String = UUID.randomUUID().toString(),
        remotePeerId: String? = null
    ) {
        if (this.session != null && this.session?.topic != session.topic) {
            killSession()
        }
        this.session = session
        this.sessionUrl = session.toUri()
        this.peerMeta = peerMeta
        this.peerId = peerId
        this.remotePeerId = remotePeerId
        reConnectSocket(session.bridge)
    }

    private fun reConnectSocket(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()
        socket = httpClient.newWebSocket(request, this)
    }

    fun approveSession(accounts: List<String>, chainId: Int): Boolean {
        check(handshakeId > 0) { "handshakeId must be greater than 0 on session approve" }
        this.chainId = chainId.toString()
        val result = WCApproveSessionResponse(
            chainId = chainId,
            accounts = accounts,
            peerId = peerId,
            peerMeta = peerMeta
        )
        val response = JsonRpcResponse(
            id = handshakeId,
            result = result
        )
        return encryptAndSend(gson.toJson(response))
    }

    fun switchChain(id: Long? = null, chainId: Int, chainName: String = "") {
        this.chainId = "$chainId"
        requestUpdateSession()
        val response = JsonRpcResponse(
            id = id ?: generateId(),
            result = chainId
        )
        encryptAndSend(gson.toJson(response))
    }

    fun requestUpdateSession(
        accounts: List<String>? = null,
        chainId: Int? = null,
        approved: Boolean = true
    ): Boolean {
        val request = JsonRpcRequest(
            id = generateId(),
            method = WCMethod.SESSION_UPDATE,
            params = listOf(
                WCSessionUpdate(
                    approved = approved,
                    chainId = chainId ?: this.chainId?.toIntOrNull(),
                    accounts = accounts
                )
            )
        )
        this.chainId = chainId.toString()
        return encryptAndSend(gson.toJson(request))
    }

    fun rejectSession(message: String = "Session rejected"): Boolean {
        check(handshakeId > 0) { "handshakeId must be greater than 0 on session reject" }
        val response = JsonRpcErrorResponse(
            id = handshakeId,
            error = JsonRpcError.serverError(
                message = message
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    fun killSession(): Boolean {
        requestUpdateSession(approved = false)
        return disconnect()
    }

    fun reConnectSession(session: WCSessionStoreItem?) {
        if (session != null) {
            resetState()
            this.session = session.session
            this.chainId = "${session.chainId}"
            peerId = session.peerId
            remotePeerId = session.remotePeerId
            peerMeta = session.remotePeerMeta
            if (socket == null) {
                reConnectSocket(session.session.bridge)
            }
            sessionUrl = session.session.toUri()
            requestUpdateSession(approved = true)
        } else {
            onFailure(sessionUrl, Throwable("WCSessionStoreItem is null"))
        }
    }

    fun <T> approveRequest(id: Long, result: T): Boolean {
        val response = JsonRpcResponse(
            id = id,
            result = result
        )
        return encryptAndSend(gson.toJson(response))
    }

    fun rejectRequest(id: Long, message: String = "Reject by the user"): Boolean {
        val response = JsonRpcErrorResponse(
            id = id,
            error = JsonRpcError.serverError(
                message = message
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    private fun decryptMessage(text: String): String {
        val message = gson.fromJson<WCSocketMessage>(text)
        val encrypted = gson.fromJson<WCEncryptionPayload>(message.payload)
        val session =
            this.session ?: throw IllegalStateException("session can't be null on message receive")
        return String(
            WCCipher.decrypt(encrypted, session.key.hexStringToByteArray()),
            Charsets.UTF_8
        )
    }

    private fun invalidParams(id: Long): Boolean {
        val response = JsonRpcErrorResponse(
            id = id,
            error = JsonRpcError.invalidParams(
                message = "Invalid parameters"
            )
        )
        return encryptAndSend(gson.toJson(response))
    }

    private fun handleMessage(payload: String) {
        try {
            val request = gson.fromJson<JsonRpcRequest<JsonArray>>(
                payload,
                typeToken<JsonRpcRequest<JsonArray>>()
            )
            val method = request.method
            if (method != null) {
                handleRequest(request)
            } else {
                onCustomRequest(sessionUrl, request.id, payload)
            }
        } catch (e: InvalidJsonRpcParamsException) {
            invalidParams(e.requestId)
        }
    }

    private fun handleRequest(request: JsonRpcRequest<JsonArray>) {
        when (request.method) {
            WCMethod.SESSION_REQUEST -> {
                val param = gson.fromJson<List<WCSessionRequest>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                handshakeId = request.id
                remotePeerId = param.peerId
                var chainIdInt = (chainId ?: "0").toIntOrNull() ?: 0
                if (param.chain.isNullOrEmpty()) {
                    chainIdInt = 1
                }
                chainId = "$chainIdInt"
                if (session != null) {
                    val info = WCRequestInfo(
                        "$chainIdInt", param.chain ?: "", WCSessionStoreItem(
                            session!!, chainIdInt, peerId ?: "",
                            remotePeerId ?: "", param.peerMeta,
                            false, Date()
                        )
                    )
                    onSessionRequest(sessionUrl, request.id, info)
                }
            }
            WCMethod.SESSION_UPDATE -> {
                val param = gson.fromJson<List<WCSessionUpdate>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                if (!param.approved) {
                    killSession()
                    onDisconnect(sessionUrl, 0, "")
                }
            }
            WCMethod.ETH_SIGN -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)
                onEthSign(
                    sessionUrl,
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.MESSAGE)
                )
            }
            WCMethod.ETH_PERSONAL_SIGN -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)
                onEthSign(
                    sessionUrl,
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE)
                )
            }
            WCMethod.ETH_SIGN_TYPE_DATA,
            WCMethod.ETH_SIGN_TYPE_DATA_V4 -> {
                val params = gson.fromJson<List<String>>(request.params)
                if (params.size < 2)
                    throw InvalidJsonRpcParamsException(request.id)
                onEthSign(
                    sessionUrl,
                    request.id,
                    WCEthereumSignMessage(params, WCEthereumSignMessage.WCSignType.TYPED_MESSAGE)
                )
            }
            WCMethod.ETH_SIGN_TRANSACTION -> {
                val param = gson.fromJson<List<WCEthereumTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onEthSignTransaction(sessionUrl, request.id, param)
            }
            WCMethod.ETH_SEND_TRANSACTION -> {
                val param = gson.fromJson<List<WCEthereumTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onEthSendTransaction(sessionUrl, request.id, param)
            }
            WCMethod.GET_ACCOUNTS -> {
                onGetAccounts(sessionUrl, request.id)
            }
            WCMethod.SIGN_TRANSACTION -> {
                val param = gson.fromJson<List<WCSignTransaction>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onSignTransaction(sessionUrl, request.id, param)
            }
            WCMethod.WALLET_SWITCH_NETWORK -> {
                val param = gson.fromJson<List<WCChangeNetwork>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                val chainId = param.chainIdHex.removePrefix("0x").toInt(10)
                val chainName = param.chainName ?: ""
                onWalletChangeNetwork(sessionUrl, request.id, chainId, chainName)
            }
            WCMethod.WALLET_ADD_NETWORK -> {
                val param = gson.fromJson<List<WCAddNetwork>>(request.params)
                    .firstOrNull() ?: throw InvalidJsonRpcParamsException(request.id)
                onWalletAddNetwork(sessionUrl, request.id, param)
            }
            else -> {}
        }
    }

    private fun subscribe(topic: String): Boolean {
        val message = WCSocketMessage(
            topic = topic,
            type = MessageType.SUB,
            payload = ""
        )
        val json = gson.toJson(message)
        Log.d(TAG, "==> subscribe $json")
        return socket?.send(gson.toJson(message)) ?: false
    }

    private fun encryptAndSend(result: String): Boolean {
        Log.d(TAG, "==> message $result")
        val session =
            this.session ?: throw IllegalStateException("session can't be null on message send")
        val payload = gson.toJson(
            WCCipher.encrypt(
                result.toByteArray(Charsets.UTF_8),
                session.key.hexStringToByteArray()
            )
        )
        val message = WCSocketMessage(
            // Once the remotePeerId is defined, all messages must be sent to this channel. The session.topic channel
            // will be used only to respond the session request message.
            topic = remotePeerId ?: session.topic,
            type = MessageType.PUB,
            payload = payload
        )
        val json = gson.toJson(message)
        Log.d(TAG, "==> encrypted $json")
        return socket?.send(json) ?: false
    }


    fun disconnect(): Boolean {
        return socket?.close(WS_CLOSE_NORMAL, null) ?: false
    }

    fun addSocketListener(listener: WebSocketListener) {
        socketListeners.add(listener)
    }

    fun removeSocketListener(listener: WebSocketListener) {
        socketListeners.remove(listener)
    }

    fun addConnectListener(listener: WalletConnectCallback) {
        connectListeners.add(listener)
    }

    fun removeConnectListener(listener: WalletConnectCallback) {
        connectListeners.remove(listener)
    }

    private fun resetState() {
        handshakeId = -1
        isConnected = false
        session = null
        peerId = null
        remotePeerId = null
        peerMeta = null
        sessionUrl = ""
    }

    fun Any?.generateId(): Long {
        return Date().time
    }
}


