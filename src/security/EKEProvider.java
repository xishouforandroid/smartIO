package security;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

/**
 * @author Abhisek Maiti
 * @author Sayantan Majumdar
 */

public class EKEProvider {

    private SecretKey mSecretKey;
    private PublicKey mPublicKey;
    private byte[] mIV;

    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    private static final String MESSAGE_DIGEST = "SHA-256";
    private static final String KEY_GENERATION_ALGORITHM = "EC";
    private static final String KEY_AGREEMENT_ALGORITHM = "ECDH";
    private static final String SECURITY_PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    private static final String ECC_CURVE_NAME = "brainpoolp256r1";
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String CIPHER_MODE = "AES/GCM/NoPadding";

    private static final Date NOT_BEFORE = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
    private static final Date NOT_AFTER = new Date(System.currentTimeMillis() + 63072000000L);

    static {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    public EKEProvider() {
        try {
            KeyPair keyPair = null;
            if (KeyStoreManager.keyStoreExists()) {
                keyPair = KeyStoreManager.getKSKeyPair();
                if (keyPair != null) mPublicKey = keyPair.getPublic();
            }
            if (keyPair == null) {
                keyPair = generateECKeys();
                if (keyPair != null) {
                    mPublicKey = keyPair.getPublic();
                    PrivateKey privateKey = keyPair.getPrivate();
                    Certificate certificate = genSelfSignedCert(mPublicKey, privateKey);
                    new KeyStoreManager().setMasterKey(privateKey, certificate);
                }
            }
        } catch (CertificateException | NoSuchAlgorithmException
                | KeyStoreException | IOException | OperatorCreationException | UnrecoverableEntryException e) {
            e.printStackTrace();
        }
    }

    public EKEProvider(String pairingKey, byte[] clientPublicKey) {
        MessageDigest msgDigest;
        try {
            msgDigest = MessageDigest.getInstance(MESSAGE_DIGEST, SECURITY_PROVIDER);
            mIV = msgDigest.digest(pairingKey.getBytes());
            KeyPair keyPair = KeyStoreManager.getKSKeyPair();
            if (keyPair != null) {
                X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(clientPublicKey));
                KeyFactory keyFactory = KeyFactory.getInstance(KEY_GENERATION_ALGORITHM, SECURITY_PROVIDER);
                mSecretKey = generateSharedSecret(keyPair.getPrivate(), keyFactory.generatePublic(pubKeySpec));
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | CertificateException | KeyStoreException | UnrecoverableEntryException
                | IOException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    private static String generatePairingKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder(new BigInteger(30, random).toString(32));
        for (int i = 0; i < stringBuilder.length(); i++) {
            char ch = stringBuilder.charAt(i);
            if (Character.isLetter(ch) && Character.isLowerCase(ch) && random.nextFloat() < 0.5) {
                stringBuilder.setCharAt(i, Character.toUpperCase(ch));
            }
        }
        return stringBuilder.toString();
    }

    public static String getPairingKey() {
        return generatePairingKey();
    }

    public byte[] getBase64EncodedPubKey() {
        if (mPublicKey != null) {
            return Base64.encodeBase64(mPublicKey.getEncoded());
        }
        return null;
    }

    private KeyPair generateECKeys() {
        try {
            ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec(ECC_CURVE_NAME);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM, SECURITY_PROVIDER);
            keyPairGenerator.initialize(parameterSpec);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        }
    }

    private SecretKey generateSharedSecret(PrivateKey privateKey, PublicKey publicKey) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM, SECURITY_PROVIDER);
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            return keyAgreement.generateSecret(ENCRYPTION_ALGORITHM);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String encryptString(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_MODE, SECURITY_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, mSecretKey, new IvParameterSpec(mIV));
            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            return new String(Base64.encodeBase64(cipherText));
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decryptString(String cipherText) {
        try {
            Key decryptionKey = new SecretKeySpec(mSecretKey.getEncoded(),
                    mSecretKey.getAlgorithm());
            IvParameterSpec ivSpec = new IvParameterSpec(mIV);
            Cipher cipher = Cipher.getInstance(CIPHER_MODE, SECURITY_PROVIDER);
            byte[] cipherTextBytes = Base64.decodeBase64(cipherText.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, decryptionKey, ivSpec);
            return new String(cipher.doFinal(cipherTextBytes), "UTF-8");
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException
                | UnsupportedEncodingException | NullPointerException e) {
            return null;
        }
    }

    private Certificate genSelfSignedCert(PublicKey publicKey, PrivateKey privateKey)
            throws OperatorCreationException, CertificateException {
        X500Principal issuer = new X500Principal("CN=127.0.0.1");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        X509v3CertificateBuilder certGen =
                new JcaX509v3CertificateBuilder(issuer, serial, NOT_BEFORE, NOT_AFTER,
                        issuer, publicKey);
        X509CertificateHolder certHolder = certGen.build(new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                        .setProvider(SECURITY_PROVIDER)
                        .build(privateKey));
        return new JcaX509CertificateConverter()
                .setProvider(SECURITY_PROVIDER)
                .getCertificate(certHolder);
    }
}


