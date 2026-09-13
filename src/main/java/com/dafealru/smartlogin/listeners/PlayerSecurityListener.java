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
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            // Cancel chat completely to prevent leaking password into public chat
            event.setCancelled(true);
            player.sendMessage(plugin.getLocaleManager().getComponent("error-chat-locked", player));
            return;
        }

        // Accidental password leak prevention for authenticated players
        if (plugin.getStreamerManager() != null && plugin.getStreamerManager().isSuspiciousChat(event.getMessage())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Security</bold></gradient> <dark_gray>»</dark_gray> <red>¡Tu mensaje fue bloqueado automáticamente para evitar que tu contraseña se publique en el chat público!</red>"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;

        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (cmd.equalsIgnoreCase("/smartlogin") || cmd.equalsIgnoreCase("/sl") || cmd.equalsIgnoreCase("/lang") || cmd.equalsIgnoreCase("/language") || cmd.equalsIgnoreCase("/idioma")) {
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
            if (event.getItemDrop() != null && event.getItemDrop().isValid()) {
                event.getItemDrop().remove();
            }
            event.getPlayer().updateInventory();
            return;
        }

        // Prevent dropping the 2FA QR map or Guide book
        if (event.getItemDrop() != null) {
            org.bukkit.inventory.ItemStack dropped = event.getItemDrop().getItemStack();
            if (plugin.getQrMapManager().isQrMap(dropped) || plugin.getGhostInventoryManager().isGuideBook(dropped)) {
                event.setCancelled(true);
                if (event.getItemDrop().isValid()) {
                    event.getItemDrop().remove();
                }
                event.getPlayer().updateInventory();
                event.getPlayer().sendMessage(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>No puedes tirar este ítem de seguridad al suelo.</red>"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                event.setCancelled(true);
                event.setTarget(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTargetGeneral(org.bukkit.event.entity.EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                event.setCancelled(true);
                event.setTarget(null);
            }
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
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
                event.setCancelled(true);
                if (event.getDamager() instanceof org.bukkit.entity.Mob mob) {
                    mob.setTarget(null);
                }
            }
        }
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
                return;
            }

            // Prevent placing or moving the 2FA QR map or Guide Book into other inventories or dropping it outside
            org.bukkit.inventory.ItemStack current = event.getCurrentItem();
            org.bukkit.inventory.ItemStack cursor = event.getCursor();
            if (plugin.getQrMapManager().isQrMap(current) || plugin.getQrMapManager().isQrMap(cursor) || plugin.getGhostInventoryManager().isGuideBook(current) || plugin.getGhostInventoryManager().isGuideBook(cursor)) {
                if (event.getRawSlot() == -999 || (event.getClickedInventory() != null && event.getClickedInventory() != player.getInventory())) {
                    event.setCancelled(true);
                    player.updateInventory();
                    player.sendMessage(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>No puedes almacenar ni tirar este ítem de seguridad.</red>"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        org.bukkit.inventory.ItemStack item = event.getItem();
        if (item != null && plugin.getGhostInventoryManager() != null && plugin.getGhostInventoryManager().isGuideBook(item)) {
            event.setCancelled(true);
            plugin.getGhostInventoryManager().openGuideBook(player);
            return;
        }

        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Prevent placing QR map or Guide Book in ItemFrames or ArmorStands
        if (event.getRightClicked() instanceof org.bukkit.entity.ItemFrame || event.getRightClicked() instanceof org.bukkit.entity.ArmorStand) {
            org.bukkit.inventory.ItemStack main = event.getPlayer().getInventory().getItemInMainHand();
            org.bukkit.inventory.ItemStack off = event.getPlayer().getInventory().getItemInOffHand();
            if (plugin.getQrMapManager().isQrMap(main) || plugin.getQrMapManager().isQrMap(off) || plugin.getGhostInventoryManager().isGuideBook(main) || plugin.getGhostInventoryManager().isGuideBook(off)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getLocaleManager().parse("<gradient:#EF4444:#F87171><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <red>No puedes colocar este ítem de seguridad en marcos ni soportes.</red>"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        // Automatically delete 2FA QR map upon death so it is never dropped on the ground
        event.getDrops().removeIf(item -> plugin.getQrMapManager().isQrMap(item));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortal(PlayerPortalEvent event) {
        if (!plugin.getAuthManager().isAuthenticated(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        boolean isAdmin = player.isOp() || player.hasPermission("smartlogin.admin");

        // Filter out plugin namespaced commands (e.g. smartlogin:*, sl:*, slogin:*) from all clients
        event.getCommands().removeIf(cmd -> cmd.startsWith("smartlogin:") || cmd.startsWith("sl:") || cmd.startsWith("slogin:"));

        // If not admin, hide administrative command names completely
        if (!isAdmin) {
            event.getCommands().remove("smartlogin");
            event.getCommands().remove("sl");
            event.getCommands().remove("slogin");
        }

        // If unauthenticated, only show whitelisted auth commands
        if (!plugin.getAuthManager().isAuthenticated(player.getUniqueId())) {
            var allowedList = plugin.getModularConfig().getConfig().getStringList("lockdown.allowed-commands");
            event.getCommands().removeIf(cmd -> !allowedList.contains("/" + cmd.toLowerCase()) 
                    && !cmd.equalsIgnoreCase("login") 
                    && !cmd.equalsIgnoreCase("register") 
                    && !cmd.equalsIgnoreCase("l") 
                    && !cmd.equalsIgnoreCase("reg")
                    && !cmd.equalsIgnoreCase("changepassword")
                    && !cmd.equalsIgnoreCase("2fa")
                    && !cmd.equalsIgnoreCase("link")
                    && !cmd.equalsIgnoreCase("tlink")
                    && !cmd.equalsIgnoreCase("lang")
                    && !cmd.equalsIgnoreCase("language")
                    && !cmd.equalsIgnoreCase("idioma"));
        }
    }
}
