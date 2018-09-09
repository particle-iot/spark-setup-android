package io.particle.android.sdk.utils

import okio.ByteString
import org.spongycastle.asn1.ASN1InputStream
import org.spongycastle.asn1.ASN1Integer
import org.spongycastle.asn1.ASN1Primitive
import org.spongycastle.asn1.DLSequence
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPublicKeySpec
import java.util.*
import javax.annotation.ParametersAreNonnullByDefault
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException


@ParametersAreNonnullByDefault
object Crypto {


    private val log = TLog.get(Crypto::class.java)

    // I'm ignoring this.  There isn't going to be an Android
    // implementation without RSA.  (In fact, I'm fairly certain
    // that the CDD *requires* it.)
    private val rsaKeyFactory: KeyFactory
        get() {
            try {
                return KeyFactory.getInstance("RSA")
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException(
                        "This should be impossible, but there is no RSA impl on this device", e)
            }

        }


    class CryptoException(cause: Throwable) : Exception(cause)


    @Throws(Crypto.CryptoException::class)
    fun readPublicKeyFromHexEncodedDerString(hexString: String): PublicKey {
        val rawBytes = ByteString.decodeHex(hexString).toByteArray()
        return buildPublicKey(rawBytes)
    }

    @Throws(Crypto.CryptoException::class)
    fun encryptAndEncodeToHex(inputString: String, publicKey: PublicKey): String {
        val utf8 = Charset.forName("UTF-8")
        val asBytes = inputString.toByteArray(utf8)
        val encryptedBytes = encryptWithKey(asBytes, publicKey)
        val hex = ByteString.of(*encryptedBytes).hex()
        // forcing lowercase here because of a bug in the early firmware that didn't accept
        // hex encoding in uppercase
        return hex.toLowerCase(Locale.ROOT)
    }

    @Throws(Crypto.CryptoException::class)
    internal fun encryptWithKey(inputData: ByteArray, publicKey: PublicKey): ByteArray {
        try {
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            return cipher.doFinal(inputData)

        } catch (e: NoSuchAlgorithmException) {
            log.e("Error while encrypting bytes: ", e)
            throw CryptoException(e)
        } catch (e: NoSuchPaddingException) {
            log.e("Error while encrypting bytes: ", e)
            throw CryptoException(e)
        } catch (e: IllegalBlockSizeException) {
            log.e("Error while encrypting bytes: ", e)
            throw CryptoException(e)
        } catch (e: InvalidKeyException) {
            log.e("Error while encrypting bytes: ", e)
            throw CryptoException(e)
        } catch (e: BadPaddingException) {
            log.e("Error while encrypting bytes: ", e)
            throw CryptoException(e)
        }

    }

    @Throws(Crypto.CryptoException::class)
    internal fun buildPublicKey(rawBytes: ByteArray): PublicKey {
        try {
            //FIXME replacing X509EncodedKeySpec because of problem with 8.1
            //Since 8.1 Bouncycastle cryptography was replaced with implementation from Conscrypt
            //https://developer.android.com/about/versions/oreo/android-8.1.html
            //either it's a bug in Conscrypt, our public key DER structure or use of X509EncodedKeySpec changed
            //alternative needed as this adds expensive Spongycastle dependence
            val bIn = ASN1InputStream(ByteArrayInputStream(rawBytes))
            val info = SubjectPublicKeyInfo
                    .getInstance(ASN1InputStream(bIn.readObject().encoded).readObject())
            val dlSequence = ASN1Primitive.fromByteArray(info.publicKeyData.bytes) as DLSequence
            val modulus = (dlSequence.getObjectAt(0) as ASN1Integer).positiveValue
            val exponent = (dlSequence.getObjectAt(1) as ASN1Integer).positiveValue

            val spec = RSAPublicKeySpec(modulus, exponent)
            val kf = rsaKeyFactory
            return kf.generatePublic(spec)
        } catch (e: InvalidKeySpecException) {
            throw CryptoException(e)
        } catch (e: IOException) {
            throw CryptoException(e)
        }

    }

}
