package com.primeevents.plugin.commands;

import com.primeevents.plugin.PrimeEvents;
import com.primeevents.plugin.util.MessageManager;
import com.primeevents.plugin.util.WebhookUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final PrimeEvents plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList("gift", "give", "reload", "webhooktest");

    public EventCommand(PrimeEvents plugin) {
        this.plugin = plugin;
    }

    private MessageManager msg() {
        return plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(msg().get("help-header"));
            if (sender.hasPermission("primeevents.admin")) {
                sender.sendMessage(msg().get("help-gift-admin"));
                sender.sendMessage(msg().get("help-give"));
                sender.sendMessage(msg().get("help-webhooktest"));
                sender.sendMessage(msg().get("help-reload"));
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gift": {
                if (!sender.hasPermission("primeevents.admin")) {
                    sender.sendMessage(msg().get("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(msg().get("players-only"));
                    return true;
                }
                Player p = (Player) sender;
                p.openInventory(plugin.getGiveAllEvent().buildEditorGui());
                p.sendMessage(msg().get("gift-editor-opened"));
                return true;
            }
            case "give": {
                if (!sender.hasPermission("primeevents.admin")) {
                    sender.sendMessage(msg().get("no-permission"));
                    return true;
                }
                int count = plugin.getGiveAllEvent().buildRewardItems().size();
                if (count == 0) {
                    sender.sendMessage(msg().get("giveall-give-empty"));
                    return true;
                }
                plugin.getGiveAllEvent().trigger();
                sender.sendMessage(msg().get("giveall-give-done", "%count%", count));
                return true;
            }
            case "webhooktest": {
                if (!sender.hasPermission("primeevents.admin")) {
                    sender.sendMessage(msg().get("no-permission"));
                    return true;
                }
                if (!plugin.getConfig().getBoolean("webhook.enabled", false)) {
                    sender.sendMessage(msg().get("webhook-disabled"));
                    return true;
                }
                sender.sendMessage(msg().get("webhook-testing"));
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    String result = WebhookUtil.sendTestSync(plugin);
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(msg().get("webhook-test-result", "%result%", result)));
                });
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("primeevents.admin")) {
                    sender.sendMessage(msg().get("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getMessageManager().load();
                plugin.getRewardStorage().reload();

                // config.yml gercekten okunabildi mi kontrol et. YAML syntax hatasi
                // (yanlis girinti, tirnaksiz ozel karakter, tab karakteri vb) varsa
                // Bukkit sessizce bos/eksik bir config doner ve konsola SEVERE hata basar,
                // komut bunu burada yakalayip acikca bildiriyor.
                if (!plugin.getConfig().contains("giveall")) {
                    sender.sendMessage(ChatColor.RED + "[UYARI] config.yml okunamadi ya da 'giveall' bolumu bulunamadi!");
                    sender.sendMessage(ChatColor.RED + "Konsolda SEVERE / \"Cannot load config.yml\" hatasi olabilir - YAML girintisini/karakterlerini kontrol et.");
                    plugin.getLogger().warning("[reload] config.yml 'giveall' bolumunu icermiyor - dosya bozuk veya eksik olabilir!");
                }

                sender.sendMessage(msg().get("config-reloaded"));

                // guncel yuklenen degerleri goster, boylece dosyadaki degisikligin
                // gercekten alinip alinmadigini gozle dogrulayabilirsin
                sender.sendMessage(ChatColor.GRAY + "--- Su anki yuklu degerler ---");
                sender.sendMessage(ChatColor.GRAY + "language: " + ChatColor.WHITE + plugin.getConfig().getString("language", "tr"));
                sender.sendMessage(ChatColor.GRAY + "giveall.enabled: " + ChatColor.WHITE + plugin.getConfig().getBoolean("giveall.enabled", true));
                sender.sendMessage(ChatColor.GRAY + "giveall.times: " + ChatColor.WHITE + plugin.getConfig().getStringList("giveall.times"));
                sender.sendMessage(ChatColor.GRAY + "giveall.broadcast-message: " + ChatColor.WHITE + plugin.getConfig().getString("giveall.broadcast-message", "(bulunamadi)"));
                sender.sendMessage(ChatColor.GRAY + "webhook.enabled: " + ChatColor.WHITE + plugin.getConfig().getBoolean("webhook.enabled", false));
                sender.sendMessage(ChatColor.GRAY + "rewards.yml odul sayisi: " + ChatColor.WHITE + plugin.getRewardStorage().getRewards().size());
                return true;
            }
            default:
                sender.sendMessage(msg().get("unknown-subcommand"));
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String s : SUBCOMMANDS) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            return out;
        }
        return new ArrayList<>();
    }
}
