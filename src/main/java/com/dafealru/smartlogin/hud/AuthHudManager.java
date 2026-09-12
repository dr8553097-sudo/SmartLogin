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

        String titleText = isRegister ? "⏳ Registrate: /register <contraseña> <repetir>" : "⏳ Inicia Sesión: /login <contraseña>";
        BossBar bar = BossBar.bossBar(
                plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>" + titleText + "</bold></gradient>"),
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
                    bar.color(BossBar.Color.PURPLE);
                }

                // Actionbar prompt
                if (plugin.getModularConfig().getConfig().getBoolean("hud-and-immersion.actionbar-prompt", true)) {
                    player.sendActionBar(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Tiempo de autenticación restante: <#F5D0FE><bold>" + remaining + "s</bold></#F5D0FE></#E9D5FF>"));
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
