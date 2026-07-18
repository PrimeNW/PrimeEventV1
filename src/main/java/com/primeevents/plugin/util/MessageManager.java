package com.primeevents.plugin.util;

import com.primeevents.plugin.PrimeEvents;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * config.yml -> language: tr/en/br ayarina gore lang/xx.yml dosyasini yukler.
 * Sadece plugin-ici sabit mesajlar (komut yardimlari, izin uyarilari vb) icindir.
 * Etkinlik mesajlari (broadcast-message, win-message vb) hala config.yml'de,
 * admin onlari zaten istedigi dilde yaziyor.
 */
public class MessageManager {

    private static final List<String> SUPPORTED = Arrays.asList("tr", "en", "br");

    private final PrimeEvents plugin;
    private YamlConfiguration messages;
    private String currentLang;

    public MessageManager(PrimeEvents plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        // desteklenen TUM dil dosyalarini diske cikar (jar icinde ama disk'te yoksa),
        // boylece en.yml ve br.yml da her zaman plugins/PrimeEvents/lang/ altinda gorunur
        // ve istenirse elle duzenlenebilir - sadece aktif dil degil.
        for (String supportedLang : SUPPORTED) {
            File f = new File(plugin.getDataFolder(), "lang/" + supportedLang + ".yml");
            if (!f.exists()) {
                plugin.saveResource("lang/" + supportedLang + ".yml", false);
            }
        }

        String lang = plugin.getConfig().getString("language", "tr").toLowerCase();
        if (!SUPPORTED.contains(lang)) {
            plugin.getLogger().warning("Desteklenmeyen dil: '" + lang + "', 'tr' kullaniliyor. Destekliler: " + SUPPORTED);
            lang = "tr";
        }
        this.currentLang = lang;

        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(langFile);

        // jar icindeki guncel varsayilanlari fallback olarak ekle (eksik key'ler icin)
        InputStream defStream = plugin.getResource("lang/" + lang + ".yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
        }

        plugin.getLogger().info("Dil yuklendi: " + lang + " (lang/ klasorunde " + SUPPORTED + " hepsi mevcut)");
    }

    public String get(String key) {
        String raw = messages.getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String get(String key, Object... replacements) {
        String result = get(key);
        // replacements: %placeholder1%, value1, %placeholder2%, value2 ...
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }
        return result;
    }

    public String getLang() {
        return currentLang;
    }
}
