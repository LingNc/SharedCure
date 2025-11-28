package com.lingnc.sharedcure.listener;

import com.lingnc.sharedcure.VillagerManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SharedCureListener implements Listener {

    private final VillagerManager manager;

    public SharedCureListener(VillagerManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Villager villager) {
                manager.checkAndTagVillager(villager);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Villager villager) {
            if (!manager.hasTag(villager)) {
                manager.setTag(villager, VillagerManager.STATUS_NOT_CURED);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.CURED) {
            return;
        }
        if (event.getTransformedEntity() instanceof Villager villager) {
            manager.setTag(villager, VillagerManager.STATUS_CURED);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return; // Avoid double firing for off-hand interactions
        }

        if (event.getRightClicked() instanceof Villager villager) {
            if (!manager.hasTag(villager)) {
                manager.checkAndTagVillager(villager);
            }

            Byte status = manager.getTag(villager);
            if (status != null && status == VillagerManager.STATUS_CURED) {
                manager.applyDiscount(villager, event.getPlayer());
            }
        }
    }
}
