package com.dafealru.smartlogin.listeners;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.auth.AuthManager;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.time.Duration;

public class PlayerConnectionListener implements Listener {

    private final SmartLogin plugin;

    public PlayerConnectionListener(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getAuthManager().setState(player.getUniqueId(), AuthManager.AuthState.UNAUTHENTICATED);

        // 1. Apply Blindness
        if (plugin.getConfigManager().isBlindnessEnabled()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        }

        // 2. Load Profile Asynchronously
        plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                plugin.getAuthManager().cacheProfile(profile);

                // Check Tri-Mode: Bedrock Auto-Login
                if (plugin.getAutoLoginDetector().isBedrock(player)) {
                    authenticatePlayer(player, profile, "tri_mode_bedrock");
                    return;
                }

                // Check Tri-Mode: Java Premium Auto-Login
                if (profile != null && profile.isPremium() && plugin.getAutoLoginDetector().isJavaPremium(player)) {
                    authenticatePlayer(player, profile, "tri_mode_premium");
                    return;
                }

                // Check Session Shield (IP auto-login cache)
                if (plugin.getSessionShield().isSessionValid(player, profile)) {
                    authenticatePlayer(player, profile, "login_success");
                    return;
                }

                // Unregistered or needs login
                if (profile == null || profile.getPasswordHash() == null) {
                    player.sendMessage(plugin.getLocaleManager().getMessage("register_prompt"));
                    showAuthTitle(player, "<gold><bold>REGISTER</bold></gold>", "<yellow>/register <password> <confirm></yellow>");
                } else if (profile.is2FAEnabled()) {
                    plugin.getAuthManager().setState(player.getUniqueId(), AuthManager.AuthState.AWAITING_2FA);
                    player.sendMessage(plugin.getLocaleManager().getMessage("two_factor_prompt"));
                    showAuthTitle(player, "<aqua><bold>2FA REQUIRED</bold></aqua>", "<gray>/2fa verify <code></gray>");
                } else {
                    player.sendMessage(plugin.getLocaleManager().getMessage("login_prompt"));
                    showAuthTitle(player, "<gradient:#9d4edd:#00f0ff><bold>LOGIN</bold></gradient>", "<yellow>/login <password></yellow>");
                }

                // Timeout Kick Task
                startTimeoutTask(player);
            });
        });
    }

    private void authenticatePlayer(Player player, PlayerProfile profile, String successKey) {
        plugin.getAuthManager().setState(player.getUniqueId(), AuthManager.AuthState.LOGGED_IN);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.sendMessage(plugin.getLocaleManager().getMessage(successKey, "player", player.getName()));

        if (profile != null) {
            profile.setLastIp(plugin.getSessionShield().getPlayerIp(player));
            profile.setLastLoginTimestamp(System.currentTimeMillis());
            plugin.getDatabaseManager().saveProfile(profile);
        }
    }

    private void showAuthTitle(Player player, String main, String sub) {
        Title title = Title.title(
                plugin.getLocaleManager().parse(main),
                plugin.getLocaleManager().parse(sub),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(4), Duration.ofMillis(500))
        );
        player.showTitle(title);
    }

    private void startTimeoutTask(Player player) {
        int timeoutSeconds = plugin.getConfigManager().getTimeoutSeconds();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                player.kick(plugin.getLocaleManager().getMessage("timeout_kick"));
            }
        }, timeoutSeconds * 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getAuthManager().uncache(player.getUniqueId());
        plugin.getQrMapManager().removePending(player.getUniqueId());
    }
}
