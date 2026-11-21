package com.campusgigs.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Salted password hashing with PBKDF2 (JDK only, no extra dependency).
 * <p>
 * Stored format: {@code pbkdf2$<iterations>$<saltBase64>$<hashBase64>}, using
 * PBKDF2WithHmacSHA256, 120000 iterations, a 16 byte random salt and a 32 byte key.
 * The iteration count is stored with each hash, so older hashes keep verifying if
 * the default changes later. Passwords are never logged or printed.
 */
public final class PasswordHasher {

    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * Hashes a password with a new random salt.
     *
     * @param password the plain password, must not be {@code null}
     * @return the value to store in {@code Users.password_hash}
     */
    public static String hash(String password) {
        Objects.requireNonNull(password, "password");
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password, salt, ITERATIONS);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + base64.encodeToString(salt) + "$" + base64.encodeToString(key);
    }

    /**
     * Checks a password against a stored hash, comparing in constant time.
     *
     * @return {@code true} only when the password matches; {@code false} for a
     *         wrong password and for {@code null} or malformed input (never throws)
     */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$", -1);
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (iterations <= 0 || salt.length == 0 || expected.length != KEY_BYTES) {
                return false;
            }
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHM + " is not available in this JDK", e);
        } finally {
            spec.clearPassword();
        }
    }
}
