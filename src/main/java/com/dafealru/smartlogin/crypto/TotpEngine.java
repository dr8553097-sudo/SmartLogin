package com.dafealru.smartlogin.crypto;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * RFC 6238 Standard TOTP (Time-Based One-Time Password) Engine.
 * Fully compatible with Google Authenticator, Authy, Microsoft Authenticator.
 */
public class TotpEngine {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateSecret() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(BASE32_CHARS.charAt(RANDOM.nextInt(BASE32_CHARS.length())));
        }
        return sb.toString();
    }

    public static String getTotpUri(String issuer, String accountName, String secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer.replace(" ", "%20"),
                accountName.replace(" ", "%20"),
                secret,
                issuer.replace(" ", "%20"));
    }

    public static boolean verifyCode(String secret, String inputCode) {
        if (secret == null || inputCode == null || inputCode.length() != 6) {
            return false;
        }

        try {
            long code = Long.parseLong(inputCode.trim());
            long currentWindow = System.currentTimeMillis() / 1000L / 30L;

            // Check 30s window drift (-1, 0, +1)
            for (int i = -1; i <= 1; i++) {
                long hash = generateTotp(secret, currentWindow + i);
                if (hash == code) {
                    return true;
                }
            }
        } catch (NumberFormatException ignored) {}
        return false;
    }

    private static long generateTotp(String secret, long timeWindow) {
        byte[] key = decodeBase32(secret);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (timeWindow & 0xFF);
            timeWindow >>= 8;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "RAW"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }
            truncatedHash &= 0x7FFFFFFF;
            return truncatedHash % 1000000;
        } catch (GeneralSecurityException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static byte[] decodeBase32(String base32) {
        base32 = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] bytes = new byte[base32.length() * 5 / 8];
        int val = 0, valb = 0, out = 0;
        for (char c : base32.toCharArray()) {
            int d = BASE32_CHARS.indexOf(c);
            val = (val << 5) | d;
            valb += 5;
            if (valb >= 8) {
                bytes[out++] = (byte) ((val >> (valb - 8)) & 0xFF);
                valb -= 8;
            }
        }
        return bytes;
    }
}
