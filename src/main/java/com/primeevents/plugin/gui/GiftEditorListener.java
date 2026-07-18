package com.primeevents.plugin.gui;

import com.primeevents.plugin.PrimeEvents;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * /event gift ile acilan editor GUI'sini dinler.
 * Sadece OP/yetkili acabilir; icine koydugu itemler kapatinca AYRI bir dosyaya (rewards.yml)
 * kaydedilir - config.yml'e KESINLIKLE dokunulmaz. Zamanlanmis GiveAll etkinliginde
 * tum oyunculara bu itemler dagitilir.
 */
public class GiftEditorListener implements Listener {

    private final PrimeEvents plugin;
    public static final String EDITOR_TITLE_KEY = "giveall.editor-title";

    public GiftEditorListener(PrimeEvents plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String expectedTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(EDITOR_TITLE_KEY, "&8Odul Duzenleyici"));
        if (e.getView().getTitle() == null || !e.getView().getTitle().equals(expectedTitle)) return;

        Inventory inv = e.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        boolean debug = plugin.getConfig().getBoolean("debug", false);
        int previousCount = plugin.getRewardStorage().getRewards().size();
        if (debug) {
            plugin.getLogger().info("[debug][gift-editor] editor kapatildi: yeni=" + items.size() + " item, onceki=" + previousCount + " item (rewards.yml)");
        }

        // Guvenlik: editor bos kapatilirsa (0 item) ve onceden kayitli odul varsa,
        // yanlislikla tum odulleri silmemek icin kaydetmiyoruz.
        if (items.isEmpty() && previousCount > 0) {
            plugin.getLogger().warning("[gift-editor] bos envanter kaydedilmeye calisildi, onceki " + previousCount + " odul KORUNDU (kaydedilmedi).");
            if (e.getPlayer() instanceof Player) {
                ((Player) e.getPlayer()).sendMessage(plugin.getMessageManager().get("gift-editor-empty-skip"));
            }
            return;
        }

        // NOT: config.yml'e HIC dokunulmuyor, sadece rewards.yml'e yaziliyor.
        plugin.getRewardStorage().setRewards(items);

        if (e.getPlayer() instanceof Player) {
            ((Player) e.getPlayer()).sendMessage(
                    plugin.getMessageManager().get("gift-editor-saved", "%count%", items.size()));
        }
    }
}
