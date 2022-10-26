package com.imtianx.walletconnect.models.ethereum

data class WCEthereumSignMessage(
    val raw: List<String>,
    val type: WCSignType
) {
    enum class WCSignType(val desc: String) {
        SIGNMESSAGE("signMessage"),
        SIGNPERSONALMESSAGE("signPersonalMessage"),
        SIGNTYPEDMESSAGE("signTypedMessage");
    }

    /**
     * Raw parameters will always be the message and the addess. Depending on the WCSignType,
     * those parameters can be swapped as description below:
     *
     *  - MESSAGE: `[address, data ]`
     *  - TYPED_MESSAGE: `[address, data]`
     *  - SIGNPERSONALMESSAGE: `[data, address]`
     *
     *  reference: https://docs.walletconnect.org/json-rpc/ethereum#eth_signtypeddata
     */
    val data
        get() = when (type) {
            WCSignType.SIGNMESSAGE -> raw[1]
            WCSignType.SIGNTYPEDMESSAGE -> raw[1]
            WCSignType.SIGNPERSONALMESSAGE -> raw[0]
        }

    val isTypeMessage
        get() = this.type == WCSignType.SIGNTYPEDMESSAGE
}