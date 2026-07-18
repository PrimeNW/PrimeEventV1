package com.primeevents.plugin.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RewardUtil {

    public static ItemStack fromSection(ConfigurationSection section) {
        if (section == null) return null;
        Material mat = Material.matchMaterial(section.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(mat, amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name", null);
            if (name != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            List<String> loreRaw = section.getStringList("lore");
            if (loreRaw != null && !loreRaw.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String line : loreRaw) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
