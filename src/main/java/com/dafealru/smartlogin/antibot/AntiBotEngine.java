package com.dafealru.smartlogin.antibot;

import com.dafealru.smartlogin.SmartLogin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Enterprise AntiBot Shield Suite 2026.
 * Zero-lag in-memory connection throttling, panic attack mitigation, name entropy analysis, and ping verification.
 */
public class AntiBotEngine implements Listener {

    private final SmartLogin plugin;

    // Rate Limiting
    private final Map<String, AtomicInteger> ipConnectionCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> ipLastJoinTime = new ConcurrentHashMap<>();
    private final Set<String> pingedIps = ConcurrentHashMap.newKeySet();
    private final AtomicInteger globalConnectionCounter = new AtomicInteger(0);

    private volatile boolean panicModeActive = false;
    private volatile long panicModeExpiry = 0;

    private static final Pattern BOT_NAME_PATTERN = Pattern.compile("^(?:MC_?Bot|Bot_?[0-9]+|Player_?[0-9]{4,}|[a-zA-Z0-9]{16})$", Pattern.CASE_INSENSITIVE);

    public AntiBotEngine(SmartLogin plugin) {
        this.plugin = plugin;
        startResetTask();
    }

    private void startResetTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int rate = globalConnectionCounter.getAndSet(0);
            ipConnectionCounts.clear();

            // Auto-trigger Panic Attack Mitigation if global connection burst > 12/sec
            if (rate >= 12) {
                if (!panicModeActive) {
                    panicModeActive = true;
                    panicModeExpiry = System.currentTimeMillis() + (30 * 1000L); // 30s attack mode
                    plugin.getLogger().warning("[AntiBot SHIELD] 🚨 ¡Alerta de Ataque Bot detectada (" + rate + " conexiones/s)! Activando Modo Pánico...");
                    plugin.getDiscordManager().sendWebhookAlert("🚨 [ANTIBOT] ATAQUE DETECTADO", "Se detectó un ataque de bots con **" + rate + " conexiones/s**. El servidor activó automáticamente el **Modo Pánico Anti-Bot**.", 0xEF4444);
                }
            } else if (panicModeActive && System.currentTimeMillis() > panicModeExpiry) {
                panicModeActive = false;
                plugin.getLogger().info("[AntiBot SHIELD] ✔ Ataque mitigado. Modo Pánico desactivado.");
            }
        }, 20L, 20L); // Every 1 second
    }

    public boolean isPanicMode() {
        return panicModeActive;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerListPing(ServerListPingEvent event) {
        String ip = event.getAddress().getHostAddress();
        pingedIps.add(ip);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLoginFilter(AsyncPlayerPreLoginEvent event) {
        String username = event.getName();
        String ip = event.getAddress().getHostAddress();

        // Whitelist / localhost / config bypass
        var ipBypassList = plugin.getModularConfig().getConfig().getStringList("general.ip-limit-bypass-users");
        var ipBypassIps = plugin.getModularConfig().getConfig().getStringList("general.ip-limit-bypass-ips");
        if ((!ipBypassList.isEmpty() && ipBypassList.stream().anyMatch(u -> u.equalsIgnoreCase(username))) ||
            ipBypassIps.contains(ip)) {
            return;
        }

        globalConnectionCounter.incrementAndGet();

        // 1. IP Join Rate Limiter (Max 3 connections per second per IP)
        AtomicInteger count = ipConnectionCounts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > 3) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>AntiBot Shield</bold></gradient>\n\n<red>Demasiadas conexiones simultáneas desde tu IP. Por favor espera unos segundos.</red>"));
            return;
        }

        // 2. Rapid Reconnect Throttler (Min 800ms between attempts)
        Long lastJoin = ipLastJoinTime.get(ip);
        long now = System.currentTimeMillis();
        if (lastJoin != null && (now - lastJoin) < 800L) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>AntiBot Shield</bold></gradient>\n\n<red>Conexión demasiado rápida. Espera 1 segundo para reconectar.</red>"));
            return;
        }
        ipLastJoinTime.put(ip, now);

        // 3. Panic Mode Filtering
        if (panicModeActive) {
            // Require Ping before join in panic mode
            if (!pingedIps.contains(ip)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>AntiBot Shield — Modo Pánico</bold></gradient>\n\n<yellow>El servidor está bajo mitigación de ataque.\nPor favor actualiza tu lista de servidores (Refresh) e ingresa nuevamente.</yellow>"));
                return;
            }

            // Reject typical bot naming patterns
            if (BOT_NAME_PATTERN.matcher(username).matches() || calculateNameEntropy(username) > 3.8) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>AntiBot Shield</bold></gradient>\n\n<red>Nombre de usuario sospechoso bloqueado durante ataque.</red>"));
                return;
            }
        }

        // 4. VPN / Proxy Check if enabled in config
        boolean blockVpn = plugin.getModularConfig().getConfig().getBoolean("antibot.block-vpn-proxy", false);
        if (blockVpn) {
            var geo = plugin.getGeoIpManager().lookup(ip).join();
            if (geo != null && geo.isVpnOrProxy()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>AntiBot Shield</bold></gradient>\n\n<red>El uso de VPN / Proxy o centros de datos no está permitido en este servidor.</red>"));
            }
        }
    }

    private double calculateNameEntropy(String str) {
        if (str == null || str.isEmpty()) return 0;
        Map<Character, Integer> freq = new java.util.HashMap<>();
        for (char c : str.toCharArray()) {
            freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        double entropy = 0;
        int len = str.length();
        for (int count : freq.values()) {
            double p = (double) count / len;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}
