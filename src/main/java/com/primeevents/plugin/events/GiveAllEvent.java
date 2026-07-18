package com.primeevents.plugin.events;

import com.primeevents.plugin.PrimeEvents;
import com.primeevents.plugin.util.RewardUtil;
import com.primeevents.plugin.util.WebhookUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

public class GiveAllEvent {

    private final PrimeEvents plugin;
    // ayni dakika icinde birden fazla tetiklenmeyi engellemek icin
    private String lastTriggeredMinute = "";
    private String lastPreAnnouncedMinute = "";
    private int currentUtcDay = -1;

    public GiveAllEvent(PrimeEvents plugin) {
        this.plugin = plugin;
    }

    /** Her 20 saniyede bir cagrilir, UTC saatini config'teki saatlerle karsilastirir. */
    public void checkSchedule() {
        if (!plugin.getConfig().getBoolean("giveall.enabled", true)) return;

        LocalTime nowUtc = LocalTime.now(ZoneOffset.UTC);
        int today = LocalDate.now(ZoneOffset.UTC).getDayOfYear();
        if (today != currentUtcDay) {
            currentUtcDay = today;
        }

        String nowStr = String.format("%02d:%02d", nowUtc.getHour(), nowUtc.getMinute());
        List<String> times = plugin.getConfig().getStringList("giveall.times");

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[debug][giveall] now(UTC)=" + nowStr + " times=" + times
                    + " rewards=" + buildRewardItems().size());
        }

        if (times.contains(nowStr) && !nowStr.equals(lastTriggeredMinute)) {
            lastTriggeredMinute = nowStr;
            trigger();
        }

        checkPreAnnounce(nowStr, times);
    }

    /** giveall.times saatlerinden webhook.pre-announce.minutes-before kadar once webhook'a "yaklasiyor" duyurusu atar. */
    private void checkPreAnnounce(String nowStr, List<String> times) {
        if (!plugin.getConfig().getBoolean("webhook.pre-announce.enabled", false)) return;
        int minutesBefore = plugin.getConfig().getInt("webhook.pre-announce.minutes-before", 10);
        if (minutesBefore <= 0) return;

        for (String time : times) {
            String preTime = subtractMinutes(time, minutesBefore);
            if (preTime == null) continue;

            if (preTime.equals(nowStr) && !nowStr.equals(lastPreAnnouncedMinute)) {
                lastPreAnnouncedMinute = nowStr;

                String title = plugin.getConfig().getString("webhook.pre-announce.title", "Yaklasan Etkinlik");
                String color = plugin.getConfig().getString("webhook.pre-announce.color", "#3498DB");
                String message = plugin.getConfig().getString("webhook.pre-announce.message", "%minutes% dakika kaldi!")
                        .replace("%minutes%", String.valueOf(minutesBefore));

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[debug][giveall] pre-announce tetiklendi, hedef=" + time + " simdi=" + nowStr);
                }

                WebhookUtil.send(plugin, "giveall-pre-announce", message, title, color);
            }
        }
    }

    /** "HH:mm" formatindaki saatten belirtilen dakikayi cikarir, gun sinirini basitce sarar (23:55 gibi). */
    private String subtractMinutes(String hhmm, int minutes) {
        try {
            String[] parts = hhmm.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int totalMinutes = ((h * 60 + m) - minutes + 24 * 60) % (24 * 60);
            int newH = totalMinutes / 60;
            int newM = totalMinutes % 60;
            return String.format("%02d:%02d", newH, newM);
        } catch (Exception e) {
            plugin.getLogger().warning("giveall.times icinde gecersiz saat formati: '" + hhmm + "' (beklenen: HH:mm)");
            return null;
        }
    }

    /**
     * Etkinligi tetikler: OP'lerin /event gift editorunde ayarladigi itemleri
     * o an online olan herkese verir.
     */
    public void trigger() {
        List<ItemStack> rewards = buildRewardItems();
        if (rewards.isEmpty()) {
            plugin.getLogger().warning(plugin.getMessageManager().get("giveall-no-rewards-warning"));
            return;
        }

        String msg = plugin.getConfig().getString("giveall.broadcast-message", "&6Odul zamani!");
        Bukkit.broadcastMessage(RewardUtil.color(msg));
        WebhookUtil.send(plugin, "giveall-trigger", msg, null, null, rewards.size());

        for (Player p : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : rewards) {
                giveOrDrop(p, item);
            }
        }
    }

    /** Odulleri artik config.yml'den degil, ayri rewards.yml dosyasindan okur (OP editorden kaydedilenler). */
    public List<ItemStack> buildRewardItems() {
        return plugin.getRewardStorage().getRewards();
    }

    /** OP icin editor GUI'sini olusturur, mevcut kayitli odulleri gosterir. */
    public Inventory buildEditorGui() {
        List<ItemStack> rewards = buildRewardItems();
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("giveall.editor-title", "&8Odul Duzenleyici"));
        Inventory inv = Bukkit.createInventory(null, 27, title);
        for (int i = 0; i < rewards.size() && i < inv.getSize(); i++) {
            inv.setItem(i, rewards.get(i));
        }
        return inv;
    }

    private void giveOrDrop(Player p, ItemStack item) {
        java.util.Map<Integer, ItemStack> leftover = p.getInventory().addItem(item.clone());
        for (ItemStack left : leftover.values()) {
            p.getWorld().dropItem(p.getLocation(), left);
        }
    }
}
