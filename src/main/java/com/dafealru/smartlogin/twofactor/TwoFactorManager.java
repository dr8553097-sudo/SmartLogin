package com.dafealru.smartlogin.twofactor;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.TotpEngine;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TwoFactorManager {

    private final SmartLogin plugin;

    public TwoFactorManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isStaffEnforced(Player player, PlayerProfile profile) {
        boolean enforce = plugin.getModularConfig().getTwoFactorConfig().getBoolean("staff-enforcement.enabled", true);
        String permission = plugin.getModularConfig().getTwoFactorConfig().getString("staff-enforcement.permission", "smartlogin.staff");
        
        if (enforce && player.hasPermission(permission)) {
            return profile == null || !profile.is2FAEnabled();
        }
        return false;
    }

    public List<String> setupNew2FA(PlayerProfile profile) {
        String secret = TotpEngine.generateBase32Secret();
        profile.setTotpSecret(secret);
        
        int codeCount = plugin.getModularConfig().getTwoFactorConfig().getInt("recovery-codes.code-count", 5);
        int codeLength = plugin.getModularConfig().getTwoFactorConfig().getInt("recovery-codes.code-length", 8);
        List<String> plainCodes = BackupCodeManager.generateRecoveryCodes(codeCount, codeLength);

        String hashedCombined = plainCodes.stream()
                .map(BackupCodeManager::hashRecoveryCode)
                .collect(Collectors.joining(","));
        profile.setBackupCodes(hashedCombined);

        return plainCodes;
    }
}
