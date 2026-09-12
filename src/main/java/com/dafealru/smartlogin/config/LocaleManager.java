package com.dafealru.smartlogin.config;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocaleManager {

    private final SmartLogin plugin;
    private final Map<String, FileConfiguration> languageFiles = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String defaultLanguage = "es";
    private boolean autoDetect = true;

    public LocaleManager(SmartLogin plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    public void loadLanguages() {
        languageFiles.clear();
        defaultLanguage = plugin.getModularConfig().getConfig().getString("general.default-language", "es").toLowerCase();
        autoDetect = plugin.getModularConfig().getConfig().getBoolean("general.auto-detect-client-language", true);

        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        String[] bundled = {"en.yml", "es.yml", "fr.yml", "pt.yml", "de.yml", "ru.yml", "zh.yml"};
        for (String b : bundled) {
            File f = new File(langDir, b);
            if (!f.exists()) {
                try {
                    plugin.saveResource("lang/" + b, false);
                } catch (Exception ignored) {}
            }
            String langCode = b.replace(".yml", "");
            if (f.exists()) {
                languageFiles.put(langCode, YamlConfiguration.loadConfiguration(f));
            }
        }
        plugin.getLogger().info("Loaded " + languageFiles.size() + " language profiles with client auto-detection.");
    }

    public String getPlayerLanguage(Player player) {
        if (player == null || !autoDetect) {
            return defaultLanguage;
        }
        try {
            Locale clientLocale = player.locale();
            if (clientLocale != null) {
                String lang = clientLocale.getLanguage().toLowerCase();
                if (languageFiles.containsKey(lang)) {
                    return lang;
                }
            }
        } catch (Throwable ignored) {}
        return defaultLanguage;
    }

    public String getRawMessage(String key, String lang) {
        FileConfiguration config = languageFiles.getOrDefault(lang, languageFiles.get(defaultLanguage));
        if (config == null) return key;
        return config.getString(key, key);
    }

    public String getRawMessage(String key, Player player) {
        return getRawMessage(key, getPlayerLanguage(player));
    }

    public Component getComponent(String key, Player player, Map<String, String> placeholders) {
        String raw = getRawMessage(key, player);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace(entry.getKey(), entry.getValue());
            }
        }
        // Support modern MiniMessage format and fallback legacy & codes
        if (raw.contains("<") && raw.contains(">")) {
            return miniMessage.deserialize(raw);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public Component getComponent(String key, Player player) {
        return getComponent(key, player, null);
    }

    public String getLegacyMessage(String key, Player player, Map<String, String> placeholders) {
        return LegacyComponentSerializer.legacySection().serialize(getComponent(key, player, placeholders));
    }

    public String getLegacyMessage(String key, Player player) {
        return getLegacyMessage(key, player, null);
    }

    public Component parse(String raw) {
        if (raw == null) return Component.empty();
        if (raw.contains("<") && raw.contains(">")) {
            return miniMessage.deserialize(raw);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public boolean setDefaultLanguage(String lang) {
        String code = lang.toLowerCase();
        if (languageFiles.containsKey(code)) {
            this.defaultLanguage = code;
            plugin.getModularConfig().getConfig().set("general.default-language", code);
            plugin.getModularConfig().saveConfig();
            return true;
        }
        return false;
    }

    public java.util.Set<String> getAvailableLanguages() {
        return languageFiles.keySet();
    }

    public String getDefaultLanguage() { return defaultLanguage; }
}
