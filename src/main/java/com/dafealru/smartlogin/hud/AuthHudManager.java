package com.dafealru.smartlogin.hud;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthHudManager {

    private final SmartLogin plugin;
    private final Map<UUID, BossBar> playerBars = new HashMap<>();
    private final Map<UUID, Integer> remainingSeconds = new HashMap<>();
    private final Map<UUID, Boolean> playerRegisterMode = new HashMap<>();
    private BukkitTask tickerTask;

    public AuthHudManager(SmartLogin plugin) {
        this.plugin = plugin;
        startTicker();
    }

    public void startHud(Player player, boolean isRegister) {
        playerRegisterMode.put(player.getUniqueId(), isRegister);

        // Show immediate localized title
        String titleKey = isRegister ? "title-register" : "title-login";
        String subtitleKey = isRegister ? "subtitle-register" : "subtitle-login";
        Title initialTitle = Title.title(
                plugin.getLocaleManager().getComponent(titleKey, player),
                plugin.getLocaleManager().getComponent(subtitleKey, player),
                Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(1500), java.time.Duration.ZERO)
        );
        player.showTitle(initialTitle);

        if (!plugin.getModularConfig().getConfig().getBoolean("hud-and-immersion.bossbar-countdown", true)) return;

        int totalTimeout = plugin.getModularConfig().getConfig().getInt("general.auth-timeout-seconds", 60);
        remainingSeconds.put(player.getUniqueId(), totalTimeout);

        String bossbarKey = isRegister ? "bossbar-register" : "bossbar-login";
        BossBar bar = BossBar.bossBar(
                plugin.getLocaleManager().getComponent(bossbarKey, player),
                1.0f,
                BossBar.Color.PURPLE,
                BossBar.Overlay.PROGRESS
        );

        playerBars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
    }

    public void stopHud(Player player) {
        BossBar bar = playerBars.remove(player.getUniqueId());
        remainingSeconds.remove(player.getUniqueId());
        playerRegisterMode.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
        player.clearTitle();
    }

    public void playSuccessSound(Player player) {
        if (!plugin.getModularConfig().getConfig().getBoolean("hud-and-immersion.play-sounds", true)) return;
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
    }

    public void playErrorSound(Player player) {
        if (!plugin.getModularConfig().getConfig().getBoolean("hud-and-immersion.play-sounds", true)) return;
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    public void play2faCompleteSound(Player player) {
        if (!plugin.getModularConfig().getConfig().getBoolean("hud-and-immersion.play-sounds", true)) return;
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.5f);
    }

    private void startTicker() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int total = plugin.getModularConfig().getConfig().getInt("general.auth-timeout-seconds", 60);

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) continue;

                // 1. Maintain persistent, seamless localized title on screen
                Boolean isReg = playerRegisterMode.get(player.getUniqueId());
                if (isReg != null) {
                    String titleKey = isReg ? "title-register" : "title-login";
                    String subtitleKey = isReg ? "subtitle-register" : "subtitle-login";
                    Title continuousTitle = Title.title(
                            plugin.getLocaleManager().getComponent(titleKey, player),
                            plugin.getLocaleManager().getComponent(subtitleKey, player),
                            Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofMillis(1500), java.time.Duration.ZERO)
                    );
                    player.showTitle(continuousTitle);
                }

                Integer remaining = remainingSeconds.get(player.getUniqueId());
                if (remaining == null) continue;

                remaining--;
                if (remaining <= 0) {
                    stopHud(player);
                    player.kick(plugin.getLocaleManager().getComponent("error-auth-timeout", player));
                    continue;
                }

                remainingSeconds.put(player.getUniqueId(), remaining);
                BossBar bar = playerBars.get(player.getUniqueId());
                if (bar != null) {
                    float progress = Math.max(0.0f, Math.min(1.0f, (float) remaining / (float) total));
                    bar.progress(progress);
                    String bossbarKey = (isReg != null && isReg) ? "bossbar-register" : "bossbar-login";
                    bar.name(plugin.getLocaleManager().getComponent(bossbarKey, player));
                    bar.color(BossBar.Color.PURPLE);
                }

                // Actionbar prompt
                if (plugin.getModularConfig().getConfig().getBoolean("hud-and-immersion.actionbar-prompt", true)) {
                    Map<String, String> placeholders = Map.of("{seconds}", String.valueOf(remaining));
                    player.sendActionBar(plugin.getLocaleManager().getComponent("actionbar-timeout", player, placeholders));
                }
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (tickerTask != null) tickerTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            stopHud(player);
        }
    }
}

