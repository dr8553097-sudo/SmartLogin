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
import java.util.UUID;

public class LocaleManager {

    private final SmartLogin plugin;
    private final Map<String, FileConfiguration> languageFiles = new HashMap<>();
    private final Map<UUID, String> playerLanguageOverrides = new java.util.concurrent.ConcurrentHashMap<>();
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
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                try (java.io.InputStream is = plugin.getResource("lang/" + b)) {
                    if (is != null) {
                        yaml.setDefaults(YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)));
                    }
                } catch (Exception ignored) {}
                languageFiles.put(langCode, yaml);
            }
        }
        plugin.getLogger().info("Loaded " + languageFiles.size() + " language profiles.");
    }

    public String getPlayerLanguage(Player player) {
        if (player == null) {
            return defaultLanguage;
        }
        String override = playerLanguageOverrides.get(player.getUniqueId());
        if (override != null && languageFiles.containsKey(override)) {
            return override;
        }
        if (autoDetect) {
            try {
                Locale clientLocale = player.locale();
                if (clientLocale != null) {
                    String lang = clientLocale.getLanguage().toLowerCase();
                    if (languageFiles.containsKey(lang)) {
                        return lang;
                    }
                }
            } catch (Throwable ignored) {}
            try {
                String loc = player.getLocale();
                if (loc != null) {
                    String lang = loc.split("_")[0].toLowerCase();
                    if (languageFiles.containsKey(lang)) {
                        return lang;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return defaultLanguage;
    }

    public void setPlayerLanguage(UUID uuid, String lang) {
        if (lang != null && languageFiles.containsKey(lang.toLowerCase())) {
            playerLanguageOverrides.put(uuid, lang.toLowerCase());
        }
    }

    public String getRawMessage(String key, String lang) {
        FileConfiguration config = languageFiles.get(lang);
        if (config != null && config.contains(key)) {
            return config.getString(key, key);
        }
        FileConfiguration defConfig = languageFiles.get(defaultLanguage);
        if (defConfig != null && defConfig.contains(key)) {
            return defConfig.getString(key, key);
        }
        FileConfiguration enConfig = languageFiles.get("en");
        if (enConfig != null && enConfig.contains(key)) {
            return enConfig.getString(key, key);
        }
        return key;
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
            this.autoDetect = false;
            plugin.getModularConfig().getConfig().set("general.default-language", code);
            plugin.getModularConfig().getConfig().set("general.auto-detect-client-language", false);
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
