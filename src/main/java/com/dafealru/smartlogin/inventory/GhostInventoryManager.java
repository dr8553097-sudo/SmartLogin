package com.dafealru.smartlogin.inventory;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ghost Inventory Engine.
 * Safely backs up and hides the player's real inventory and armor upon joining until they authenticate,
 * preventing Freecam / X-Ray / hacked client espionage in competitive servers.
 */
public class GhostInventoryManager {

    private final SmartLogin plugin;
    private final org.bukkit.NamespacedKey guideBookKey;

    public record SavedInventory(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {}

    private final Map<UUID, SavedInventory> ghostInventories = new ConcurrentHashMap<>();

    public GhostInventoryManager(SmartLogin plugin) {
        this.plugin = plugin;
        this.guideBookKey = new org.bukkit.NamespacedKey(plugin, "smartlogin_guide_book");
    }

    public boolean isGuideBook(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (item.getItemMeta().getPersistentDataContainer().has(guideBookKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
            return true;
        }
        if (item.getItemMeta().displayName() != null) {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
            return text.contains("Guía") || text.contains("Guide") || text.contains("SmartLogin");
        }
        return false;
    }

    public void openGuideBook(Player player) {
        String lang = plugin.getLocaleManager().getPlayerLanguage(player);
        boolean isEn = "en".equalsIgnoreCase(lang);

        Component page1 = plugin.getLocaleManager().parse(isEn ?
                "<gradient:#9333EA:#C084FC><bold>SmartLogin Guide</bold></gradient>\n\n" +
                "<dark_gray>Welcome to the server!</dark_gray>\n\n" +
                "<#9333EA><bold>1. Register:</bold></#9333EA>\n" +
                "<dark_gray>/register <pass> <pass></dark_gray>\n\n" +
                "<#9333EA><bold>2. Login:</bold></#9333EA>\n" +
                "<dark_gray>/login <pass></dark_gray>\n\n" +
                "<#C084FC>» Next page for 2FA.</#C084FC>" :
                "<gradient:#9333EA:#C084FC><bold>Guía SmartLogin</bold></gradient>\n\n" +
                "<dark_gray>¡Bienvenido al servidor!</dark_gray>\n\n" +
                "<#9333EA><bold>1. Registro:</bold></#9333EA>\n" +
                "<dark_gray>/register <clave> <repetir></dark_gray>\n\n" +
                "<#9333EA><bold>2. Iniciar Sesión:</bold></#9333EA>\n" +
                "<dark_gray>/login <clave></dark_gray>\n\n" +
                "<#C084FC>» Pasa página para ver 2FA.</#C084FC>"
        );

        Component page2 = plugin.getLocaleManager().parse(isEn ?
                "<gradient:#9333EA:#C084FC><bold>2FA Security</bold></gradient>\n\n" +
                "<#9333EA><bold>Google Authenticator:</bold></#9333EA>\n" +
                "<dark_gray>1. Type <#C084FC>/2fa setup</#C084FC>\n" +
                "2. Scan QR map on phone.\n" +
                "3. Type <#C084FC>/2fa verify <code></#C084FC>\n\n" +
                "<#9333EA><bold>Security Center:</bold></#9333EA>\n" +
                "<dark_gray>Open with <#C084FC>/security</#C084FC></dark_gray>" :
                "<gradient:#9333EA:#C084FC><bold>Seguridad 2FA</bold></gradient>\n\n" +
                "<#9333EA><bold>Google Authenticator:</bold></#9333EA>\n" +
                "<dark_gray>1. Escribe <#C084FC>/2fa setup</#C084FC>\n" +
                "2. Escanea el mapa QR.\n" +
                "3. Escribe <#C084FC>/2fa verify <código></#C084FC>\n\n" +
                "<#9333EA><bold>Centro de Seguridad:</bold></#9333EA>\n" +
                "<dark_gray>Abre con <#C084FC>/security</#C084FC></dark_gray>"
        );

        Component page3 = plugin.getLocaleManager().parse(isEn ?
                "<gradient:#9333EA:#C084FC><bold>Useful Commands</bold></gradient>\n\n" +
                "<#C084FC><bold>/changepassword</bold></#C084FC>\n<dark_gray>Change password</dark_gray>\n\n" +
                "<#C084FC><bold>/lang</bold></#C084FC>\n<dark_gray>Change language</dark_gray>\n\n" +
                "<#C084FC><bold>/link</bold></#C084FC>\n<dark_gray>Link Discord 2FA</dark_gray>\n\n" +
                "<#C084FC><bold>/email</bold></#C084FC>\n<dark_gray>Link Gmail rescue</dark_gray>" :
                "<gradient:#9333EA:#C084FC><bold>Comandos Útiles</bold></gradient>\n\n" +
                "<#C084FC><bold>/changepassword</bold></#C084FC>\n<dark_gray>Cambia tu clave</dark_gray>\n\n" +
                "<#C084FC><bold>/lang</bold></#C084FC>\n<dark_gray>Cambia de idioma</dark_gray>\n\n" +
                "<#C084FC><bold>/link</bold></#C084FC>\n<dark_gray>Vincula Discord 2FA</dark_gray>\n\n" +
                "<#C084FC><bold>/email</bold></#C084FC>\n<dark_gray>Vincula Gmail</dark_gray>"
        );

        net.kyori.adventure.inventory.Book book = net.kyori.adventure.inventory.Book.book(
                plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin Guía</bold></gradient>"),
                Component.text("Dafealru"),
                page1, page2, page3
        );
        player.openBook(book);
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    public void hideInventory(Player player) {
        if (player == null || plugin.getAuthManager().isAuthenticated(player.getUniqueId())) return;
        if (ghostInventories.containsKey(player.getUniqueId())) return;

        // Backup real inventory
        ItemStack[] contents = player.getInventory().getContents().clone();
        ItemStack[] armor = player.getInventory().getArmorContents().clone();
        ItemStack offhand = player.getInventory().getItemInOffHand().clone();

        ghostInventories.put(player.getUniqueId(), new SavedInventory(contents, armor, offhand));

        // Clear visual inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        // Set guide book in slot 0
        ItemStack guide = new ItemStack(Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) guide.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLocaleManager().parse("<gradient:#9333EA:#C084FC><bold>SmartLogin</bold></gradient> <dark_gray>»</dark_gray> <#E9D5FF>Guía de Autenticación</#E9D5FF> <gray>(Clic derecho)</gray>"));
            meta.setAuthor("Dafealru");
            meta.setTitle("SmartLogin Guía");
            meta.getPersistentDataContainer().set(guideBookKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
            guide.setItemMeta(meta);
        }
        player.getInventory().setItem(0, guide);
        player.updateInventory();
    }

    public void restoreInventory(Player player) {
        if (player == null) return;
        SavedInventory saved = ghostInventories.remove(player.getUniqueId());
        if (saved != null) {
            player.getInventory().setContents(saved.contents());
            player.getInventory().setArmorContents(saved.armor());
            player.getInventory().setItemInOffHand(saved.offhand());
            player.updateInventory();
        }
    }

    public void handleQuit(Player player) {
        if (player == null) return;
        SavedInventory saved = ghostInventories.remove(player.getUniqueId());
        if (saved != null) {
            player.getInventory().setContents(saved.contents());
            player.getInventory().setArmorContents(saved.armor());
            player.getInventory().setItemInOffHand(saved.offhand());
        }
    }

    public void restoreAll() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            restoreInventory(player);
        }
        ghostInventories.clear();
    }
}