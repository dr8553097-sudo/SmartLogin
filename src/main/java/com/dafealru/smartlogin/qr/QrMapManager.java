package com.dafealru.smartlogin.qr;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.TotpEngine;
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
    private final Map<UUID, String> pendingSetupSecrets = new HashMap<>();

    public QrMapManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public String start2FASetup(Player player) {
        String secret = TotpEngine.generateSecret();
        pendingSetupSecrets.put(player.getUniqueId(), secret);

        String issuer = plugin.getConfigManager().getIssuerName();
        String uri = TotpEngine.getTotpUri(issuer, player.getName(), secret);

        // Create Map View
        MapView mapView = Bukkit.createMap(player.getWorld());
        mapView.getRenderers().clear();
        mapView.addRenderer(new QrMapRenderer(uri));

        // Create Item
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setMapView(mapView);
            meta.displayName(plugin.getLocaleManager().parse("<gradient:#00f0ff:#9d4edd><bold>SmartLogin 2FA Setup Map</bold></gradient>"));
            mapItem.setItemMeta(meta);
        }

        // Place in hand
        player.getInventory().setItemInMainHand(mapItem);
        return secret;
    }

    public String getPendingSecret(UUID uuid) {
        return pendingSetupSecrets.get(uuid);
    }

    public void removePending(UUID uuid) {
        pendingSetupSecrets.remove(uuid);
    }
}
