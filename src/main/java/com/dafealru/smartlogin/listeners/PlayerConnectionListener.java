package com.dafealru.smartlogin.listeners;

import com.dafealru.smartlogin.SmartLogin;
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress().getAddress().getHostAddress();

        // Check if first-admin setup wizard is pending
        if (!plugin.getSetupWizardManager().isSetupCompleted() && (player.isOp() || player.hasPermission("smartlogin.admin"))) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getSetupWizardManager().sendSetupForm(player);
            }, 10L);
        }

        // Apply blindness / slowness / spawn teleport
        plugin.getSpawnManager().handleJoinSpawn(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 5, false, false));

        plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (profile != null) {
                    plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);

                    // Check Bedrock / Floodgate Auto-Login
                    if (plugin.getAutoLoginDetector().isBedrockPlayer(player)) {
                        plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);
                        plugin.getSpawnManager().handleLoginRestore(player);
                        player.sendMessage(plugin.getLocaleManager().getComponent("success-auto-login-bedrock", player));
                        return;
                    }

                    // Check Java Premium Auto-Login
                    if (profile.isPremium() && plugin.getAutoLoginDetector().isJavaPremiumPlayer(player)) {
                        plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);
                        plugin.getSpawnManager().handleLoginRestore(player);
                        player.sendMessage(plugin.getLocaleManager().getComponent("success-auto-login-premium", player));
                        return;
                    }

                    // Check SessionShield
                    if (plugin.getSessionShield().isSessionValid(player.getUniqueId(), ip)) {
                        plugin.getAuthManager().setAuthenticated(player.getUniqueId(), true);
                        plugin.getSpawnManager().handleLoginRestore(player);
                        player.sendMessage(plugin.getLocaleManager().getComponent("success-session-shield", player));
                        return;
                    }

                    // Send Login Prompt
                    sendAuthPrompt(player, false);
                } else {
                    // Send Register Prompt
                    sendAuthPrompt(player, true);
                }
            });
        });
    }

    private void sendAuthPrompt(Player player, boolean isRegister) {
        String titleKey = isRegister ? "title-register" : "title-login";
        String subKey = isRegister ? "subtitle-register" : "subtitle-login";
        String msgKey = isRegister ? "prompt-register" : "prompt-login";

        Title title = Title.title(
                plugin.getLocaleManager().getComponent(titleKey, player),
                plugin.getLocaleManager().getComponent(subKey, player),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
        );
        player.showTitle(title);
        player.sendMessage(plugin.getLocaleManager().getComponent(msgKey, player));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getAuthManager().removeAuthenticated(player.getUniqueId());
        plugin.getQrMapManager().cleanup(player.getUniqueId());
    }
}
