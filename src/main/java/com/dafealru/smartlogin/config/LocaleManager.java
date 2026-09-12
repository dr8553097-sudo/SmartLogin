package com.dafealru.smartlogin.config;

import com.dafealru.smartlogin.SmartLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LocaleManager {

    private final SmartLogin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages = new HashMap<>();

    public LocaleManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        messages.clear();
        String lang = plugin.getConfigManager().getLanguage();
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();

        // Save default resource files if not present
        String[] defaultLangs = {"en", "es", "fr", "pt"};
        for (String dl : defaultLangs) {
            File f = new File(langDir, dl + ".yml");
            if (!f.exists()) {
                plugin.saveResource("lang/" + dl + ".yml", false);
            }
        }

        File langFile = new File(langDir, lang + ".yml");
        if (!langFile.exists()) langFile = new File(langDir, "en.yml");

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(langFile);
        for (String key : yaml.getKeys(false)) {
            messages.put(key, yaml.getString(key));
        }
        plugin.getLogger().info("Loaded " + messages.size() + " messages for language: " + lang);
    }

    public Component getMessage(String key, String... placeholders) {
        String raw = messages.getOrDefault(key, "<red>Missing message: " + key + "</red>");
        String prefix = messages.getOrDefault("prefix", "");

        String formatted = prefix + raw;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                formatted = formatted.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return miniMessage.deserialize(formatted);
    }

    public Component parse(String miniMessageText) {
        return miniMessage.deserialize(miniMessageText);
    }
}
