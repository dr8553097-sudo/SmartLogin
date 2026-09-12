package com.dafealru.smartlogin.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Military-Grade Cryptographic Password Engine.
 * Supports Argon2-style Salted PBKDF2 with SHA-512 and timing-safe comparison.
 */
public class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 32;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 512;

    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        return hashPassword(password, salt);
    }

    public static boolean verify(String password, String salt, String expectedHash) {
        if (expectedHash == null || expectedHash.isEmpty()) return false;
        
        // 1. Standard SmartLogin PBKDF2WithHmacSHA512
        if (salt != null && !salt.isEmpty()) {
            if (verifyPassword(password, salt, expectedHash)) return true;
        }

        // 2. Legacy AuthMe SHA256 ($SHA$salt$hash)
        if (expectedHash.startsWith("$SHA$")) {
            try {
                String[] parts = expectedHash.split("\\$");
                if (parts.length == 4) {
                    String s = parts[2];
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] d1 = md.digest(password.getBytes());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : d1) sb.append(String.format("%02x", b));
                    
                    md.reset();
                    md.update((sb.toString() + s).getBytes());
                    byte[] d2 = md.digest();
                    StringBuilder sb2 = new StringBuilder();
                    for (byte b : d2) sb2.append(String.format("%02x", b));
                    return sb2.toString().equalsIgnoreCase(parts[3]);
                }
            } catch (Exception ignored) {}
        }

        // 3. Legacy AuthMe SHA512 ($SHA512$salt$hash)
        if (expectedHash.startsWith("$SHA512$")) {
            try {
                String[] parts = expectedHash.split("\\$");
                if (parts.length == 4) {
                    String s = parts[2];
                    MessageDigest md = MessageDigest.getInstance("SHA-512");
                    byte[] d1 = md.digest(password.getBytes());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : d1) sb.append(String.format("%02x", b));

                    md.reset();
                    md.update((sb.toString() + s).getBytes());
                    byte[] d2 = md.digest();
                    StringBuilder sb2 = new StringBuilder();
                    for (byte b : d2) sb2.append(String.format("%02x", b));
                    return sb2.toString().equalsIgnoreCase(parts[3]);
                }
            } catch (Exception ignored) {}
        }

        // 4. Legacy Plain SHA-256
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b));
            if (hex.toString().equalsIgnoreCase(expectedHash)) return true;
        } catch (Exception ignored) {}

        // 5. Legacy MD5 & Double MD5 (xAuth / CrazyLogin / nLogin legacy)
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5_1 = md.digest(password.getBytes());
            StringBuilder hex1 = new StringBuilder();
            for (byte b : md5_1) hex1.append(String.format("%02x", b));
            if (hex1.toString().equalsIgnoreCase(expectedHash)) return true;

            // Double MD5
            md.reset();
            byte[] md5_2 = md.digest(hex1.toString().getBytes());
            StringBuilder hex2 = new StringBuilder();
            for (byte b : md5_2) hex2.append(String.format("%02x", b));
            if (hex2.toString().equalsIgnoreCase(expectedHash)) return true;

            // Salted MD5 ($MD5$salt$hash)
            if (expectedHash.startsWith("$MD5$")) {
                String[] parts = expectedHash.split("\\$");
                if (parts.length == 4) {
                    String s = parts[2];
                    md.reset();
                    byte[] d = md.digest((hex1.toString() + s).getBytes());
                    StringBuilder sb = new StringBuilder();
                    for (byte b : d) sb.append(String.format("%02x", b));
                    if (sb.toString().equalsIgnoreCase(parts[3])) return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    public static String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return fallbackSha512(password, salt);
        }
    }

    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        String computedHash = hashPassword(password, salt);
        return MessageDigest.isEqual(computedHash.getBytes(), expectedHash.getBytes());
    }

    private static String fallbackSha512(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(Base64.getDecoder().decode(salt));
            byte[] bytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Fatal: Cryptography provider missing SHA-512", ex);
        }
    }
}
