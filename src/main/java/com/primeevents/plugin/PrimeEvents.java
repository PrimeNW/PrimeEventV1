package com.primeevents.plugin;

import com.primeevents.plugin.events.GiveAllEvent;
import com.primeevents.plugin.gui.GiftEditorListener;
import com.primeevents.plugin.commands.EventCommand;
import com.primeevents.plugin.util.MessageManager;
import com.primeevents.plugin.util.RewardStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class PrimeEvents extends JavaPlugin {

    private static PrimeEvents instance;
    private GiveAllEvent giveAllEvent;
    private MessageManager messageManager;
    private RewardStorage rewardStorage;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.messageManager = new MessageManager(this);
        this.rewardStorage = new RewardStorage(this);

        this.giveAllEvent = new GiveAllEvent(this);

        getServer().getPluginManager().registerEvents(new GiftEditorListener(this), this);

        EventCommand cmd = new EventCommand(this);
        getCommand("event").setExecutor(cmd);
        getCommand("event").setTabCompleter(cmd);

        // her 20 saniyede bir UTC saatini kontrol et (giveall zamanlamasi)
        getServer().getScheduler().runTaskTimer(this, giveAllEvent::checkSchedule, 20L, 20L * 20L);

        getLogger().info("PrimeEvents aktif edildi.");
    }

    @Override
    public void onDisable() {
        getLogger().info("PrimeEvents devre disi birakildi.");
    }

    public static PrimeEvents getInstance() {
        return instance;
    }

    public GiveAllEvent getGiveAllEvent() {
        return giveAllEvent;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public RewardStorage getRewardStorage() {
        return rewardStorage;
    }
}
