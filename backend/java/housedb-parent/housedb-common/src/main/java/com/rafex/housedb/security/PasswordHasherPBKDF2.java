package com.rafex.housedb.security;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasherPBKDF2 {

    private final int derivedKeyBytes;

    public PasswordHasherPBKDF2(final int derivedKeyBytes) {
        if (derivedKeyBytes < 16) {
            throw new IllegalArgumentException("derivedKeyBytes demasiado pequeño");
        }
        this.derivedKeyBytes = derivedKeyBytes;
    }

    public boolean verify(final char[] password, final byte[] salt, final int iterations, final byte[] expectedHash) {
        if (password == null || salt == null || expectedHash == null || iterations <= 0) {
            return false;
        }

        final var dk = derive(password, salt, iterations, expectedHash.length);
        try {
            return MessageDigest.isEqual(dk, expectedHash);
        } finally {
            Arrays.fill(dk, (byte) 0);
        }
    }

    private static byte[] derive(final char[] password, final byte[] salt, final int iterations,
            final int outLenBytes) {
        try {
            final var spec = new PBEKeySpec(password, salt, iterations, outLenBytes * 8);
            final var skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (final Exception e) {
            throw new IllegalStateException("PBKDF2 derivation failed", e);
        }
    }

    public HashResult hash(final char[] password, final byte[] salt, final int iterations) {
        final var dk = derive(password, salt, iterations, derivedKeyBytes);
        return new HashResult(dk, salt, iterations);
    }

    public record HashResult(byte[] hash, byte[] salt, int iterations) {
    }
}
