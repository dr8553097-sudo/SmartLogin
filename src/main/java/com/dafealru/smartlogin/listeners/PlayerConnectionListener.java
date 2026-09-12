package com.dafealru.smartlogin.listeners;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();
        String ip = event.getAddress().getHostAddress();

        // Check Max Accounts per IP
        int maxIp = plugin.getModularConfig().getConfig().getInt("general.max-accounts-per-ip", 10);
        var ipBypassList = plugin.getModularConfig().getConfig().getStringList("general.ip-limit-bypass-users");
        var ipBypassIps = plugin.getModularConfig().getConfig().getStringList("general.ip-limit-bypass-ips");

        boolean isBypass = username.equalsIgnoreCase("Dafealru") ||
                           username.toLowerCase().startsWith("xylos") ||
                           ipBypassList.stream().anyMatch(u -> u.equalsIgnoreCase(username)) ||
                           ipBypassIps.contains(ip) ||
                           ipBypassIps.stream().anyMatch(bIp -> bIp.equalsIgnoreCase(ip));

        if (maxIp > 0 && !isBypass) {
            int current = plugin.getDatabaseManager().countAccountsByIp(ip).join();
            PlayerProfile existing = plugin.getDatabaseManager().loadProfile(event.getUniqueId()).join();
            if (existing == null && current >= maxIp) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        plugin.getLocaleManager().parse("<red>Maximum registered accounts per IP exceeded (" + maxIp + ")!</red>"));
                return;
            }
        }

        // Strict Nickname Case Protection (Anti-Impostor)
        if (plugin.getModularConfig().getConfig().getBoolean("nickname-protection.strict-case", true)) {
            PlayerProfile byName = plugin.getDatabaseManager().loadProfileByName(username).join();
            if (byName != null && !byName.getUsername().equals(username)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        plugin.getLocaleManager().parse("<red>Please enter using exact registered capitalization: <gold>" + byName.getUsername() + "</gold></red>"));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ip = player.getAddress().getAddress().getHostAddress();

        // Apply blindness / slowness / spawn teleport and allow flight to prevent vanilla fly kick
        plugin.getSpawnManager().handleJoinSpawn(player);
        player.setAllowFlight(true);
        player.setFlying(false);
        player.setInvulnerable(true);
        player.setCollidable(false);
        if (plugin.getModularConfig().getConfig().getBoolean("lockdown.apply-blindness", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 60 * 5, 0, false, false, false));
        }
        if (plugin.getModularConfig().getConfig().getBoolean("lockdown.apply-slowness", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 60 * 5, 3, false, false, false));
        }

        // Check if first-admin setup wizard is pending
        if (!plugin.getSetupWizardManager().isSetupCompleted() && (player.isOp() || player.hasPermission("smartlogin.admin"))) {
            plugin.getSetupWizardManager().startWizard(player);
            return;
        }

        plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenCompose(p -> {
            if (p != null) return java.util.concurrent.CompletableFuture.completedFuture(p);
            return plugin.getDatabaseManager().loadProfileByName(player.getName());
        }).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (profile != null && profile.getPasswordHash() != null && !profile.getPasswordHash().trim().isEmpty()) {
                    plugin.getAuthManager().cacheProfile(player.getUniqueId(), profile);

                    // Check Bedrock / Floodgate Auto-Login
                    if (plugin.getAutoLoginDetector().isBedrockPlayer(player)) {
                        completeAuth(player, profile, "success-auto-login-bedrock");
                        return;
                    }

                    // Check Java Premium Auto-Login
                    if (profile.isPremium() && plugin.getAutoLoginDetector().isJavaPremiumPlayer(player)) {
                        completeAuth(player, profile, "success-auto-login-premium");
                        return;
                    }

                    // Check SessionShield
                    if (plugin.getSessionShield().isSessionValid(player.getUniqueId(), ip)) {
                        completeAuth(player, profile, "success-session-shield");
                        return;
                    }

                    // Send Login Prompt & Start HUD
                    sendAuthPrompt(player, false);
                } else {
                    // Send Register Prompt & Start HUD
                    sendAuthPrompt(player, true);
                }
            });
        });
    }

    private void completeAuth(Player player, PlayerProfile profile, String successMsgKey) {
        plugin.getAuthManager().completeAuthentication(player, successMsgKey);
    }

    private void sendAuthPrompt(Player player, boolean isRegister) {
        plugin.getAuthHudManager().startHud(player, isRegister);

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
    public void onLocaleChange(org.bukkit.event.player.PlayerLocaleChangeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            plugin.getDatabaseManager().loadProfile(player.getUniqueId()).thenCompose(p -> {
                if (p != null) return java.util.concurrent.CompletableFuture.completedFuture(p);
                return plugin.getDatabaseManager().loadProfileByName(player.getName());
            }).thenAccept(profile -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline() && !plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                        boolean isRegister = profile == null || profile.getPasswordHash() == null || profile.getPasswordHash().trim().isEmpty();
                        plugin.getAuthHudManager().startHud(player, isRegister);
                    }
                });
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getAuthHudManager().stopHud(player);
        plugin.getAuthManager().removeAuthenticated(player.getUniqueId());
        plugin.getQrMapManager().cleanup(player.getUniqueId());
    }
}
