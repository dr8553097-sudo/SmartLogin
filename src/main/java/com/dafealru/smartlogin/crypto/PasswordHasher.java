package com.dafealru.smartlogin.crypto;

import com.dafealru.smartlogin.SmartLogin;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Enterprise Multi-Algorithm Cryptographic Engine for SmartLogin.
 * Algorithms:
 *  - ARGON2ID (PHC Winner, Memory-Hard, OWASP Standard, GPU/ASIC Resistant) [DEFAULT]
 *  - PBKDF2_SHA512 (Standard, 65,536 iterations, high performance)
 *  - BCRYPT (Web standard, OpenBSD cost=12)
 *  - SHA256 (Salted lightweight)
 */
public class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    
    // Argon2id OWASP High-Security Parameters
    public static final int ARGON2_TYPE = Argon2Parameters.ARGON2_id;
    public static final int ARGON2_VERSION = Argon2Parameters.ARGON2_VERSION_13; // v=19 (0x13)
    public static final int ARGON2_MEMORY_KB = 65536; // 64 MB RAM
    public static final int ARGON2_ITERATIONS = 3;    // 3 passes
    public static final int ARGON2_PARALLELISM = 2;   // 2 lanes
    public static final int ARGON2_HASH_LENGTH = 32;  // 256 bits output
    public static final int SALT_LENGTH = 32;         // 256 bits salt

    // PBKDF2 parameters
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int PBKDF2_KEY_LENGTH = 512;

    // BCrypt cost
    private static final int BCRYPT_COST = 12;

    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().withoutPadding().encodeToString(salt);
    }

    /**
     * Hashes password using the server's configured primary algorithm in auth.yml.
     */
    public static String hash(String password, String salt) {
        String algo = "ARGON2ID";
        try {
            if (SmartLogin.getInstance() != null && SmartLogin.getInstance().getModularConfig() != null) {
                algo = SmartLogin.getInstance().getModularConfig().getAuthConfig().getString("hashing.algorithm", "ARGON2ID").toUpperCase();
            }
        } catch (Exception ignored) {}

        return switch (algo) {
            case "PBKDF2", "PBKDF2_SHA512" -> hashPBKDF2(password, salt);
            case "BCRYPT" -> hashBCrypt(password, salt);
            case "SHA256" -> hashSha256(password, salt);
            default -> hashArgon2(password, salt, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM);
        };
    }

    public static String hashArgon2(String password, String salt, int memoryKb, int iterations, int parallelism) {
        try {
            byte[] saltBytes;
            try {
                saltBytes = Base64.getDecoder().decode(salt);
            } catch (Exception e) {
                saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            }

            Argon2Parameters.Builder builder = new Argon2Parameters.Builder(ARGON2_TYPE)
                    .withVersion(ARGON2_VERSION)
                    .withIterations(iterations)
                    .withMemoryAsKB(memoryKb)
                    .withParallelism(parallelism)
                    .withSalt(saltBytes);

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(builder.build());

            byte[] hashBytes = new byte[ARGON2_HASH_LENGTH];
            generator.generateBytes(password.toCharArray(), hashBytes, 0, hashBytes.length);

            String encodedSalt = Base64.getEncoder().withoutPadding().encodeToString(saltBytes);
            String encodedHash = Base64.getEncoder().withoutPadding().encodeToString(hashBytes);

            return String.format("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s", memoryKb, iterations, parallelism, encodedSalt, encodedHash);
        } catch (Exception e) {
            return hashPBKDF2(password, salt);
        }
    }

    public static String hashBCrypt(String password, String salt) {
        try {
            byte[] saltBytes;
            try {
                saltBytes = Base64.getDecoder().decode(salt);
            } catch (Exception e) {
                saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            }
            byte[] b16Salt = new byte[16];
            System.arraycopy(saltBytes, 0, b16Salt, 0, Math.min(saltBytes.length, 16));
            return OpenBSDBCrypt.generate(password.toCharArray(), b16Salt, BCRYPT_COST);
        } catch (Exception e) {
            return hashPBKDF2(password, salt);
        }
    }

    public static String hashSha256(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("$SHA$").append(salt).append("$");
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return hashPBKDF2(password, salt);
        }
    }

    public static boolean isArgon2(String hash) {
        return hash != null && hash.startsWith("$argon2id$");
    }

    public static boolean isCurrentAlgorithm(String hash) {
        String algo = "ARGON2ID";
        try {
            if (SmartLogin.getInstance() != null && SmartLogin.getInstance().getModularConfig() != null) {
                algo = SmartLogin.getInstance().getModularConfig().getAuthConfig().getString("hashing.algorithm", "ARGON2ID").toUpperCase();
            }
        } catch (Exception ignored) {}

        return switch (algo) {
            case "PBKDF2", "PBKDF2_SHA512" -> hash != null && !hash.startsWith("$");
            case "BCRYPT" -> hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
            case "SHA256" -> hash != null && hash.startsWith("$SHA$");
            default -> isArgon2(hash);
        };
    }

    public static boolean verify(String password, String salt, String expectedHash) {
        if (expectedHash == null || expectedHash.trim().isEmpty() || password == null) {
            return false;
        }

        // 1. Argon2id PHC Format
        if (expectedHash.startsWith("$argon2id$")) {
            if (verifyArgon2(password, expectedHash)) return true;
        }

        // 2. BCrypt ($2a$, $2b$, $2y$)
        if (expectedHash.startsWith("$2a$") || expectedHash.startsWith("$2b$") || expectedHash.startsWith("$2y$")) {
            try {
                if (OpenBSDBCrypt.checkPassword(expectedHash, password.toCharArray())) return true;
            } catch (Exception ignored) {}
        }

        // 3. PBKDF2WithHmacSHA512
        if (salt != null && !salt.isEmpty()) {
            if (verifyPBKDF2(password, salt, expectedHash)) return true;
        }

        // 4. Legacy AuthMe SHA256 ($SHA$salt$hash)
        if (expectedHash.startsWith("$SHA$")) {
            try {
                String[] parts = expectedHash.split("\\$");
                if (parts.length == 4) {
                    String s = parts[2];
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] d1 = md.digest(password.getBytes(StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    for (byte b : d1) sb.append(String.format("%02x", b));

                    md.reset();
                    md.update((sb.toString() + s).getBytes(StandardCharsets.UTF_8));
                    byte[] d2 = md.digest();
                    StringBuilder sb2 = new StringBuilder();
                    for (byte b : d2) sb2.append(String.format("%02x", b));
                    return sb2.toString().equalsIgnoreCase(parts[3]);
                }
            } catch (Exception ignored) {}
        }

        // 5. Legacy AuthMe SHA512 ($SHA512$salt$hash)
        if (expectedHash.startsWith("$SHA512$")) {
            try {
                String[] parts = expectedHash.split("\\$");
                if (parts.length == 4) {
                    String s = parts[2];
                    MessageDigest md = MessageDigest.getInstance("SHA-512");
                    byte[] d1 = md.digest(password.getBytes(StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    for (byte b : d1) sb.append(String.format("%02x", b));

                    md.reset();
                    md.update((sb.toString() + s).getBytes(StandardCharsets.UTF_8));
                    byte[] d2 = md.digest();
                    StringBuilder sb2 = new StringBuilder();
                    for (byte b : d2) sb2.append(String.format("%02x", b));
                    return sb2.toString().equalsIgnoreCase(parts[3]);
                }
            } catch (Exception ignored) {}
        }

        // 6. Plain SHA-256
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b));
            if (hex.toString().equalsIgnoreCase(expectedHash)) return true;
        } catch (Exception ignored) {}

        // 7. MD5 & Double MD5 (xAuth / CrazyLogin / nLogin legacy)
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5_1 = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex1 = new StringBuilder();
            for (byte b : md5_1) hex1.append(String.format("%02x", b));
            if (hex1.toString().equalsIgnoreCase(expectedHash)) return true;

            md.reset();
            byte[] md5_2 = md.digest(hex1.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex2 = new StringBuilder();
            for (byte b : md5_2) hex2.append(String.format("%02x", b));
            if (hex2.toString().equalsIgnoreCase(expectedHash)) return true;

            if (expectedHash.startsWith("$MD5$")) {
                String[] parts = expectedHash.split("\\$");
                if (parts.length == 4) {
                    String s = parts[2];
                    md.reset();
                    byte[] d = md.digest((hex1.toString() + s).getBytes(StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    for (byte b : d) sb.append(String.format("%02x", b));
                    if (sb.toString().equalsIgnoreCase(parts[3])) return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

    public static boolean verifyArgon2(String password, String phcHash) {
        try {
            String[] parts = phcHash.split("\\$");
            if (parts.length < 6) return false;

            int memoryKb = ARGON2_MEMORY_KB;
            int iterations = ARGON2_ITERATIONS;
            int parallelism = ARGON2_PARALLELISM;

            String[] params = parts[3].split(",");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    if (kv[0].equals("m")) memoryKb = Integer.parseInt(kv[1]);
                    else if (kv[0].equals("t")) iterations = Integer.parseInt(kv[1]);
                    else if (kv[0].equals("p")) parallelism = Integer.parseInt(kv[1]);
                }
            }

            byte[] saltBytes = Base64.getDecoder().decode(parts[4]);
            byte[] expectedHashBytes = Base64.getDecoder().decode(parts[5]);

            Argon2Parameters.Builder builder = new Argon2Parameters.Builder(ARGON2_TYPE)
                    .withVersion(ARGON2_VERSION)
                    .withIterations(iterations)
                    .withMemoryAsKB(memoryKb)
                    .withParallelism(parallelism)
                    .withSalt(saltBytes);

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(builder.build());

            byte[] computedHashBytes = new byte[expectedHashBytes.length];
            generator.generateBytes(password.toCharArray(), computedHashBytes, 0, computedHashBytes.length);

            return MessageDigest.isEqual(computedHashBytes, expectedHashBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public static String hashPBKDF2(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return fallbackSha512(password, salt);
        }
    }

    public static boolean verifyPBKDF2(String password, String salt, String expectedHash) {
        try {
            String computedHash = hashPBKDF2(password, salt);
            return MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private static String fallbackSha512(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(Base64.getDecoder().decode(salt));
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Fatal: Cryptography provider missing SHA-512", ex);
        }
    }

    /**
     * Benchmark method measuring hashing latency in milliseconds.
     */
    public static double benchmark(String algorithm) {
        String testPass = "BenchmarkTestSecretPassword123!#";
        String salt = generateSalt();
        long start = System.nanoTime();
        switch (algorithm.toUpperCase()) {
            case "ARGON2ID" -> hashArgon2(testPass, salt, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM);
            case "PBKDF2", "PBKDF2_SHA512" -> hashPBKDF2(testPass, salt);
            case "BCRYPT" -> hashBCrypt(testPass, salt);
            case "SHA256" -> hashSha256(testPass, salt);
            default -> hashArgon2(testPass, salt, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM);
        }
        long durationNano = System.nanoTime() - start;
        return durationNano / 1_000_000.0;
    }
}
