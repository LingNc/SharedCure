package com.lingnc.sharedcure;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import java.util.Map;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public class VillagerManager {

    public static final byte STATUS_NOT_CURED = 0;
    public static final byte STATUS_CURED = 1;

    private final NamespacedKey statusKey;

    public VillagerManager(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.statusKey = new NamespacedKey(plugin, "discount_status");
    }

    public void checkAndTagVillager(Villager villager) {
        if (hasTag(villager)) {
            return;
        }

        boolean cured = checkVillagerHistory(villager);
        setTag(villager, cured ? STATUS_CURED : STATUS_NOT_CURED);
    }

    public void applyDiscount(Villager villager, Player player) {
        UUID uuid = player.getUniqueId();
        Reputation reputation = villager.getReputation(uuid);
        if (reputation == null) {
            reputation = new Reputation();
        }

        int current = reputation.getReputation(ReputationType.MAJOR_POSITIVE);
        if (current < 20) {
            reputation.setReputation(ReputationType.MAJOR_POSITIVE, 20);
            villager.setReputation(uuid, reputation);
        }
    }

    public void setTag(Villager villager, byte status) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        container.set(statusKey, PersistentDataType.BYTE, status);
    }

    public Byte getTag(Villager villager) {
        PersistentDataContainer container = villager.getPersistentDataContainer();
        if (!container.has(statusKey, PersistentDataType.BYTE)) {
            return null;
        }
        return container.get(statusKey, PersistentDataType.BYTE);
    }

    public boolean hasTag(Villager villager) {
        return villager.getPersistentDataContainer().has(statusKey, PersistentDataType.BYTE);
    }

    private boolean checkVillagerHistory(Villager villager) {
        Map<UUID, Reputation> reputationMap = villager.getReputations();
        if (reputationMap == null || reputationMap.isEmpty()) {
            return false;
        }

        for (Reputation reputation : reputationMap.values()) {
            if (reputation != null && reputation.getReputation(ReputationType.MAJOR_POSITIVE) > 0) {
                return true;
            }
        }
        return false;
    }
}
