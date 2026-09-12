package com.dafealru.smartlogin.qr;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.crypto.TotpEngine;
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
    private final Map<UUID, String> pendingSetupSecrets = new HashMap<>();

    public QrMapManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public void giveQrMap(Player player, String secret) {
        pendingSetupSecrets.put(player.getUniqueId(), secret);

        String issuer = plugin.getModularConfig().getTwoFactorConfig().getString("totp.issuer-name", "SmartLogin");
        String uri = TotpEngine.getTotpUri(issuer, player.getName(), secret);

        MapView mapView = Bukkit.createMap(player.getWorld());
        mapView.getRenderers().clear();
        mapView.addRenderer(new QrMapRenderer(uri));

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setMapView(mapView);
            meta.displayName(Component.text("📱 SmartLogin 2FA Setup QR", NamedTextColor.GOLD, TextDecoration.BOLD));
            mapItem.setItemMeta(meta);
        }

        player.getInventory().setItemInMainHand(mapItem);
    }

    public String getPendingSecret(UUID uuid) {
        return pendingSetupSecrets.get(uuid);
    }

    public void cleanup(UUID uuid) {
        pendingSetupSecrets.remove(uuid);
    }
}
