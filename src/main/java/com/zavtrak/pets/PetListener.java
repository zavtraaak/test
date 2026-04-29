package com.zavtrak.pets;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bukkit event listeners that keep pets well-behaved:
 *   - dismiss the pet when the owner logs out
 *   - prevent the pet from targeting or damaging its owner
 *   - prevent the owner from accidentally damaging their own pet
 */
public final class PetListener implements Listener {

    private final PetsPlugin plugin;

    public PetListener(PetsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPetManager().removeFor(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        PetManager mgr = plugin.getPetManager();

        // Owner trying to hurt their own pet.
        if (event.getDamager() instanceof Player player && mgr.isManagedPet(event.getEntity())) {
            mgr.getOwnerOf(event.getEntity()).ifPresent(ownerId -> {
                if (ownerId.equals(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            });
            return;
        }

        // Pet trying to hurt its owner.
        if (mgr.isManagedPet(event.getDamager()) && event.getEntity() instanceof Player player) {
            mgr.getOwnerOf(event.getDamager()).ifPresent(ownerId -> {
                if (ownerId.equals(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        PetManager mgr = plugin.getPetManager();
        if (!mgr.isManagedPet(event.getEntity())) {
            return;
        }
        if (event.getTarget() instanceof Player target) {
            mgr.getOwnerOf(event.getEntity()).ifPresent(ownerId -> {
                if (ownerId.equals(target.getUniqueId())) {
                    event.setCancelled(true);
                }
            });
        }
    }
}
