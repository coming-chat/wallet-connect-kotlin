package com.imtianx.walletconnect

import com.imtianx.walletconnect.exceptions.InvalidHmacException
import com.imtianx.walletconnect.extensions.hexStringToByteArray
import com.imtianx.walletconnect.extensions.toHex
import com.imtianx.walletconnect.models.WCEncryptionPayload
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding"
private val MAC_ALGORITHM = "HmacSHA256"

object WCCipher {
    fun encrypt(data: ByteArray, key: ByteArray): WCEncryptionPayload {
        val iv = randomBytes(16)
        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val encryptedData = cipher.doFinal(data)
        val hmac = computeHmac(
            data = encryptedData,
            iv = iv,
            key = key
        )

        return WCEncryptionPayload(
            data = encryptedData.toHex(),
            iv = iv.toHex(),
            hmac = hmac
        )
    }

    fun decrypt(payload: WCEncryptionPayload, key: ByteArray): ByteArray {
        val data = payload.data.hexStringToByteArray()
        val iv = payload.iv.hexStringToByteArray()
        val computedHmac = computeHmac(
            data = data,
            iv = iv,
            key = key
        )

        if (computedHmac != payload.hmac.toLowerCase()) {
            throw InvalidHmacException()
        }

        val keySpec = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        return cipher.doFinal(data)
    }

    private fun computeHmac(data: ByteArray, iv: ByteArray, key: ByteArray): String {
        val mac = Mac.getInstance(MAC_ALGORITHM)
        val payload = data + iv
        mac.init(SecretKeySpec(key, MAC_ALGORITHM))
        return mac.doFinal(payload).toHex()
    }

    private fun randomBytes(size: Int): ByteArray {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)

        return bytes
    }
}