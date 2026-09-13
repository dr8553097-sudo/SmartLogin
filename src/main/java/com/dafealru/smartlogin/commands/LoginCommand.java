package com.dafealru.smartlogin.commands;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.auth.AuthManager;
import com.dafealru.smartlogin.crypto.PasswordHasher;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LoginCommand implements CommandExecutor {

    private final SmartLogin plugin;
    private final Map<java.util.UUID, Integer> failedAttempts = new java.util.concurrent.ConcurrentHashMap<>();

    public LoginCommand(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLocaleManager().getComponent("error-only-players", null));
            return true;
        }

        if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            player.sendMessage(plugin.getLocaleManager().getComponent("error-already-logged-in", player));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getLocaleManager().getComponent("prompt-login", player));
            return true;
        }

        String inputPassword = args[0];

        plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenCompose(p -> {
            if (p != null) return java.util.concurrent.CompletableFuture.completedFuture(p);
            return plugin.getDatabaseManager().loadProfileByName(player.getName());
        }).thenAccept(profile -> {
            if (profile == null || profile.getPasswordHash() == null || profile.getPasswordHash().trim().isEmpty()) {
                player.sendMessage(plugin.getLocaleManager().getComponent("error-not-registered", player));
                return;
            }

            boolean valid = PasswordHasher.verify(inputPassword, profile.getSalt(), profile.getPasswordHash());

            if (valid) {
                failedAttempts.remove(player.getUniqueId());

                // Auto-upgrade legacy or different algorithm hashes to server's configured primary algorithm
                if (!PasswordHasher.isCurrentAlgorithm(profile.getPasswordHash())) {
                    String newSalt = PasswordHasher.generateSalt();
                    String newHash = PasswordHasher.hash(inputPassword, newSalt);
                    profile.setSalt(newSalt);
                    profile.setPasswordHash(newHash);
                }

                if (profile.is2FAEnabled()) {
                    plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);
                    plugin.getAuthManager().setState(player.getUniqueId(), AuthManager.AuthState.AWAITING_2FA);
                    player.sendMessage(plugin.getLocaleManager().getComponent("prompt-2fa-verify", player));
                    return;
                }

                if (player.getAddress() != null) {
                    profile.setLastIp(player.getAddress().getAddress().getHostAddress());
                }
                profile.setLastLoginTimestamp(System.currentTimeMillis());
                plugin.getDatabaseManager().saveProfile(profile);
                plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);

                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getAuthManager().completeAuthentication(player, "success-logged-in");
                });
            } else {
                int maxAttempts = plugin.getModularConfig().getAuthConfig().getInt("security.max-login-attempts", 3);
                int fails = failedAttempts.getOrDefault(player.getUniqueId(), 0) + 1;
                failedAttempts.put(player.getUniqueId(), fails);

                if (fails >= maxAttempts) {
                    failedAttempts.remove(player.getUniqueId());
                    String ip = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "desconocida";
                    
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        player.kick(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin Security</bold></gradient>\n\n<red>Has superado el límite de " + maxAttempts + " intentos fallidos de contraseña.\nPor favor espera unos minutos antes de volver a ingresar.</red>"));
                    });

                    plugin.getDiscordManager().sendWebhookAlert("🚨 [LOGIN] FUERZA BRUTA DETECTADA", "El usuario **" + player.getName() + "** (`" + ip + "`) fue expulsado tras superar " + maxAttempts + " intentos fallidos de contraseña.", 0xEF4444);
                } else {
                    int remaining = maxAttempts - fails;
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>Contraseña incorrecta. Intentos restantes: <bold>" + remaining + "/" + maxAttempts + "</bold></red>"));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
        });

        return true;
    }
}
