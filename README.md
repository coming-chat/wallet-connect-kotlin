# WalletConnect (Evm and Aptos)

[![Publish package CI](https://github.com/imtianx/wallet-connect-kotlin/actions/workflows/publish-ci.yml/badge.svg)](https://github.com/imtianx/wallet-connect-kotlin/actions/workflows/publish-ci.yml)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/imtianx/wallet-connect-kotlin)

[WalletConnect](https://walletconnect.org/) Kotlin SDK, implements 1.0.0 websocket based protocol.

## Demo
<img src="docs/demo.gif" width="250">

## Features

- [x] Connect and disconnect
- [x] Approve / Reject / Kill session
- [x] Approve and reject `eth_sign` / `personal_sign` / `eth_signTypedData`
- [x] Approve and reject `eth_signTransaction` / `eth_sendTransaction`
- [x] Approve and reject `bnb_sign` (binance dex orders)
- [x] session persistent / recovery
- [x] switch evm chain by chainId `wallet_switchEthereumChain` 
- [x] switch to aptos chain by chainName `wallet_switchEthereumChain`
- [x] Approve and reject `aptos_sign`
- [x] Approve and reject `aptos_signTransaction`
- [x] Approve and reject `aptos_sendTransaction`

>connect test simple :[wallectconnect-cc-example](https://github.com/wbh1328551759/wallectconnect-cc-example),  https://wallectconnect-cc-example.vercel.app

## Installation

Android releases are hosted on [GitHub packages](https://github.com/coming-chat/wallet-sdk-android/packages/1683567), you need to add GitHub access token to install it. Please checkout [this installation guide](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry).

```gradle
dependencies {
    implementation "org.comingchat:wallet-connect-v1:$Tag"
}
```
> replace the `Tag` in the code with [latest](https://github.com/coming-chat/wallet-sdk-android/packages/1683567)

## Usage

parse session from scanned QR code:

```kotlin
val peerMeta = WCPeerMeta(name = "App name", url = "https://website.com")
val string = "wc:..."
val session = WCSession.from(string) ?: throw InvalidSessionError // invalid session
// handle session
wcClient.connect(wcSession, peerMeta)
```

configure and handle incoming message:

```kotlin
val wcClient = WCClient(GsonBuilder(), okHttpClient)

wcClient.onDisconnect = { _, _ -> 
    onDisconnect() 
}

wcClient.onSessionRequest = { _, peer -> 
    // ask for user consent
}

wcClient.onDisconnect = { _, _ -> 
    // handle disconnect
}
wcClient.onFailure = { t -> 
    // handle failure
}
wcClient.onGetAccounts = { id -> 
    // handle get_accounts
}

wcClient.onEthSign = { id, message -> 
    // handle eth_sign, personal_sign, eth_signTypedData
}
wcClient.onEthSignTransaction = { id, transaction -> 
    // handle eth_signTransaction
}
wcClient.onEthSendTransaction = { id, transaction -> 
    // handle eth_sendTransaction
}

wcClient.onSignTransaction = { id, transaction -> 
    // handle bnb_sign
}

wcClient.onWalletChangeNetwork = { id, chainId->
    // handle wallet_switchEthereumChain
}
```

approve session

```kotlin
wcClient.approveSession(listOf(address), chainId)
```

approve request

```kotlin
wcClient.approveRequest(id, signResult) // hex formatted sign
```

disconnect

```kotlin
if (wcClient.session != null) {
    wcClient.killSession()
} else {
    wcClient.disconnect()
}
```

## License

WalletConnect is available under the MIT license. See the LICENSE file for more info.
