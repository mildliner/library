package bftsmart.demo.incident;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

public final class CryptoUtils {

    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String KEY_ALGORITHM = "EC";
    public static final String CURVE_NAME = "secp256r1";
    public static final int DIGEST_LENGTH = 32;
    private static final byte[] EMPTY_DIGEST = new byte[DIGEST_LENGTH];

    private CryptoUtils() {
    }

    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return digest.digest(input == null ? new byte[0] : input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static byte[] sha256Utf8(String input) {
        return sha256(input == null ? new byte[0] : input.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] zeroDigest() {
        return Arrays.copyOf(EMPTY_DIGEST, EMPTY_DIGEST.length);
    }

    public static KeyPair generateDeterministicEcKeyPair(String seedMaterial) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(("incident-demo|" + seedMaterial).getBytes(StandardCharsets.UTF_8));
            generator.initialize(new ECGenParameterSpec(CURVE_NAME), secureRandom);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to generate EC key pair", e);
        }
    }

    public static byte[] sign(PrivateKey privateKey, byte[] toBeSigned) {
        try {
            Signature signature = Signature.getInstance(IncidentProtocol.SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(toBeSigned);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign incident payload", e);
        }
    }

    public static boolean verify(PublicKey publicKey, byte[] toBeSigned, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance(IncidentProtocol.SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(toBeSigned);
            return signature.verify(signatureBytes == null ? new byte[0] : signatureBytes);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to verify incident signature", e);
        }
    }

    public static boolean equals(byte[] left, byte[] right) {
        return MessageDigest.isEqual(left == null ? new byte[0] : left, right == null ? new byte[0] : right);
    }
}
