package com.dafealru.smartlogin.qr;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.TotpEngine;
import com.dafealru.smartlogin.database.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QrMapManager {

    private final SmartLogin plugin;
    private final org.bukkit.NamespacedKey qrMapKey;
    private final Map<UUID, String> pendingSetupSecrets = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> expiryTasks = new HashMap<>();

    public QrMapManager(SmartLogin plugin) {
        this.plugin = plugin;
        this.qrMapKey = new org.bukkit.NamespacedKey(plugin, "smartlogin_2fa_map");
    }

    public boolean isQrMap(ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP || !item.hasItemMeta()) return false;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(qrMapKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
            return true;
        }
        if (meta.displayName() != null) {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            if (text.contains("SmartLogin") || text.contains("2FA Setup QR") || text.contains("2FA")) {
                return true;
            }
        }
        return false;
    }

    public void giveQrMap(Player player, String secret) {
        pendingSetupSecrets.put(player.getUniqueId(), secret);

        String issuer = plugin.getModularConfig().getTotpConfig().getString("totp.issuer-name", "SmartLogin");
        String uri = TotpEngine.getTotpUri(issuer, player.getName(), secret);

        MapView mapView = Bukkit.createMap(player.getWorld());
        mapView.getRenderers().clear();
        mapView.addRenderer(new QrMapRenderer(uri));

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setMapView(mapView);
            meta.displayName(Component.text("📱 SmartLogin 2FA Setup QR", NamedTextColor.GOLD, TextDecoration.BOLD));
            meta.getPersistentDataContainer().set(qrMapKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            mapItem.setItemMeta(meta);
        }

        player.getInventory().setItemInMainHand(mapItem);

        // Cancel previous expiry task if any
        org.bukkit.scheduler.BukkitTask oldTask = expiryTasks.remove(player.getUniqueId());
        if (oldTask != null) oldTask.cancel();

        // 15-minute (18,000 ticks) auto-expiration
        org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            expiryTasks.remove(player.getUniqueId());
            if (!player.isOnline()) return;

            PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
            if (profile == null || !profile.is2FAEnabled()) {
                removeQrMap(player);
                player.sendMessage(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin 2FA</bold></gradient> <dark_gray>»</dark_gray> <red>El mapa QR ha expirado tras 15 minutos sin vincular. Ejecuta <#C084FC>/2fa setup</#C084FC> si deseas volver a intentarlo.</red>"));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
            }
        }, 15L * 60L * 20L);

        expiryTasks.put(player.getUniqueId(), task);
    }

    public String getPendingSecret(UUID uuid) {
        return pendingSetupSecrets.get(uuid);
    }

    public void cleanup(UUID uuid) {
        pendingSetupSecrets.remove(uuid);
        org.bukkit.scheduler.BukkitTask task = expiryTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public void removeQrMap(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        pendingSetupSecrets.remove(uuid);
        org.bukkit.scheduler.BukkitTask task = expiryTasks.remove(uuid);
        if (task != null) task.cancel();

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isQrMap(item)) {
                player.getInventory().setItem(i, null);
            }
        }
        player.updateInventory();
    }
}
