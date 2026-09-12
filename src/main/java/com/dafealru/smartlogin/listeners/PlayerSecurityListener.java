package com.dafealru.smartlogin.listeners;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.auth.AuthManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerSecurityListener implements Listener {

    private final SmartLogin plugin;

    public PlayerSecurityListener(SmartLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY()) {
            event.setTo(from.setDirection(to.getDirection()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;

        // Cancel chat completely to prevent leaking password into public chat
        event.setCancelled(true);
        player.sendMessage(plugin.getLocaleManager().getComponent("error-chat-locked", player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;

        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (cmd.equalsIgnoreCase("/smartlogin") || cmd.equalsIgnoreCase("/sl")) {
            return;
        }

        var allowedList = plugin.getModularConfig().getConfig().getStringList("lockdown.allowed-commands");
        boolean allowed = allowedList.stream()
                .anyMatch(allowedCmd -> cmd.equalsIgnoreCase(allowedCmd) || cmd.startsWith(allowedCmd + " "));

        if (!allowed) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLocaleManager().getComponent("error-command-locked", player));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageOther(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                // Allow clicking inside PIN Pad
                if (event.getView().title().equals(plugin.getLocaleManager().parse("<dark_purple><bold>Security PIN Pad</bold></dark_purple>"))) {
                    return;
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
