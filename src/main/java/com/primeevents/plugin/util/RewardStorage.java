package com.primeevents.plugin.util;

import com.primeevents.plugin.PrimeEvents;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GiveAll odullerini config.yml'den TAMAMEN AYRI, kendi dosyasinda (rewards.yml) saklar.
 * /event gift ile odul kaydedilirken artik config.yml'e hic dokunulmuyor - webhook, dil,
 * saat gibi diger ayarlar bu islemden bagimsiz, hicbir sekilde etkilenemez.
 */
public class RewardStorage {

    private final PrimeEvents plugin;
    private final File file;
    private YamlConfiguration data;

    public RewardStorage(PrimeEvents plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rewards.yml");
        load();
        migrateFromOldConfigIfNeeded();
    }

    /** /event reload ile dis dosyayi (rewards.yml) diskten tekrar okur. */
    public void reload() {
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("rewards.yml olusturulamadi: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    /** Eski surumde oduller config.yml -> giveall.rewards altinda kalmis olabilir, bir kereye mahsus tasi. */
    @SuppressWarnings("unchecked")
    private void migrateFromOldConfigIfNeeded() {
        if (data.contains("rewards")) return; // rewards.yml zaten dolu, tasimaya gerek yok
        if (!plugin.getConfig().contains("giveall.rewards")) return;

        List<?> old = plugin.getConfig().getList("giveall.rewards");
        if (old == null || old.isEmpty()) return;

        List<ItemStack> migrated = new ArrayList<>();
        for (Object o : old) {
            if (o instanceof ItemStack) {
                migrated.add((ItemStack) o);
            } else if (o instanceof Map) {
                try {
                    migrated.add(ItemStack.deserialize((Map<String, Object>) o));
                } catch (Exception ignored) {
                    // deserialize edilemeyen eski/bozuk item, atla
                }
            }
        }

        if (!migrated.isEmpty()) {
            setRewards(migrated);
            plugin.getLogger().info("[rewards] " + migrated.size() + " eski odul config.yml'den rewards.yml'e otomatik tasindi.");
        }
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("rewards.yml kaydedilemedi: " + e.getMessage());
        }
    }

    public void setRewards(List<ItemStack> items) {
        data.set("rewards", items);
        save();
    }

    @SuppressWarnings("unchecked")
    public List<ItemStack> getRewards() {
        List<?> raw = data.getList("rewards");
        List<ItemStack> result = new ArrayList<>();
        if (raw == null) return result;
        for (Object o : raw) {
            if (o instanceof ItemStack) {
                result.add(((ItemStack) o).clone());
            } else if (o instanceof Map) {
                try {
                    result.add(ItemStack.deserialize((Map<String, Object>) o));
                } catch (Exception ex) {
                    plugin.getLogger().warning("[rewards] deserialize edilemeyen item atlandi: " + ex.getMessage());
                }
            }
        }
        return result;
    }
}
