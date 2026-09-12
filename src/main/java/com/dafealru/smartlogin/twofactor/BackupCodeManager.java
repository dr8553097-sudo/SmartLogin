package com.dafealru.smartlogin.twofactor;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class BackupCodeManager {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static List<String> generateRecoveryCodes(int count, int length) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder("SL-");
            for (int j = 0; j < length; j++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    public static String hashRecoveryCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.toUpperCase().trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return code;
        }
    }

    public static boolean verifyAndConsume(PlayerProfile profile, String inputCode) {
        String hashedInput = hashRecoveryCode(inputCode);
        String savedHashedCodes = profile.getBackupCodes();
        if (savedHashedCodes == null || savedHashedCodes.isEmpty()) return false;

        String[] list = savedHashedCodes.split(",");
        StringBuilder updated = new StringBuilder();
        boolean matched = false;

        for (String h : list) {
            String clean = h.trim();
            if (!matched && clean.equalsIgnoreCase(hashedInput)) {
                matched = true;
            } else {
                if (updated.length() > 0) updated.append(",");
                updated.append(clean);
            }
        }

        if (matched) {
            profile.setBackupCodes(updated.toString());
        }
        return matched;
    }
}
