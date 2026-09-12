package com.dafealru.smartlogin.hud;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
    private BukkitTask tickerTask;

    public AuthHudManager(SmartLogin plugin) {
        this.plugin = plugin;
        startTicker();
    }

    public void startHud(Player player, boolean isRegister) {
        if (!plugin.getModularConfig().getConfig().getBoolean("hud-and-immersion.bossbar-countdown", true)) return;

        int totalTimeout = plugin.getModularConfig().getConfig().getInt("general.auth-timeout-seconds", 60);
        remainingSeconds.put(player.getUniqueId(), totalTimeout);

        String titleText = isRegister ? "⏳ Register: /register <password> <confirm>" : "⏳ Login: /login <password>";
        BossBar bar = BossBar.bossBar(
                Component.text(titleText, NamedTextColor.GOLD, TextDecoration.BOLD),
                1.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );

        playerBars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
    }

    public void stopHud(Player player) {
        BossBar bar = playerBars.remove(player.getUniqueId());
        remainingSeconds.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
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

                    if (progress > 0.5f) {
                        bar.color(BossBar.Color.GREEN);
                    } else if (progress > 0.25f) {
                        bar.color(BossBar.Color.YELLOW);
                    } else {
                        bar.color(BossBar.Color.RED);
                    }
                }

                // Actionbar prompt
                if (plugin.getModularConfig().getConfig().getBoolean("hud-and-immersion.actionbar-prompt", true)) {
                    player.sendActionBar(Component.text("⏳ Auth Timeout: " + remaining + "s", NamedTextColor.YELLOW));
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
