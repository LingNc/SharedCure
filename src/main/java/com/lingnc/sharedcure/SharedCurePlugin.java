package com.lingnc.sharedcure;

import com.lingnc.sharedcure.listener.SharedCureListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SharedCurePlugin extends JavaPlugin {

    private VillagerManager villagerManager;

    @Override
    public void onEnable() {
        this.villagerManager = new VillagerManager(this);
        Bukkit.getPluginManager().registerEvents(new SharedCureListener(villagerManager), this);
        scanLoadedVillagers();
        getLogger().info("SharedCure enabled. All villager cures will now be shared server-wide.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SharedCure disabled. Discounts will remain stored on villagers.");
    }

    private void scanLoadedVillagers() {
        Bukkit.getWorlds().forEach(world -> world.getEntitiesByClass(org.bukkit.entity.Villager.class)
            .forEach(villagerManager::checkAndTagVillager));
    }
}
