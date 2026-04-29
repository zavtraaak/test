package com.zavtrak.pets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks the currently active pet for each player and runs the periodic
 * follow / teleport task.
 *
 * <p>Each player may own at most one active pet at a time. When a player
 * disconnects, their pet is despawned to avoid abandoned mobs cluttering
 * the world. Pets are tagged with persistent data so admin tools can
 * recognise them after the chunk unloads.</p>
 */
public final class PetManager {

    /** Distance (in blocks) above which the pet is force-teleported to the owner. */
    private static final double TELEPORT_DISTANCE_SQ = 30.0 * 30.0;
    /** Distance (in blocks) above which the pet should actively path to the owner. */
    private static final double FOLLOW_DISTANCE_SQ = 6.0 * 6.0;
    /** Pathfinder movement speed multiplier. */
    private static final double FOLLOW_SPEED = 1.2D;

    private final PetsPlugin plugin;
    private final Map<UUID, Pet> petsByOwner = new HashMap<>();

    private final NamespacedKey ownerKey;
    private final NamespacedKey typeKey;

    private BukkitTask followTask;

    public PetManager(PetsPlugin plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.typeKey = new NamespacedKey(plugin, "type");
    }

    public void start() {
        followTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::tickFollow, 20L, 20L);
    }

    public void shutdown() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        for (Pet pet : petsByOwner.values()) {
            removeEntity(pet.getEntity());
        }
        petsByOwner.clear();
    }

    /** Spawn a new pet for the given player, removing any existing one first. */
    public Pet summon(Player owner, PetType type) {
        removeFor(owner);

        Location spawn = owner.getLocation();
        LivingEntity entity = (LivingEntity) owner.getWorld().spawnEntity(spawn, type.getEntityType());

        entity.setPersistent(true);
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(false);
        entity.customName(Component.text(owner.getName() + "'s " + type.displayName(), NamedTextColor.AQUA));
        entity.setCustomNameVisible(true);

        if (entity instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(owner);
        }

        entity.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        entity.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());

        Pet pet = new Pet(owner.getUniqueId(), entity, type);
        petsByOwner.put(owner.getUniqueId(), pet);
        return pet;
    }

    /** Remove the active pet for a player, if any. */
    public boolean removeFor(Player owner) {
        return removeFor(owner.getUniqueId());
    }

    public boolean removeFor(UUID ownerId) {
        Pet existing = petsByOwner.remove(ownerId);
        if (existing == null) {
            return false;
        }
        removeEntity(existing.getEntity());
        return true;
    }

    public Optional<Pet> getPet(Player owner) {
        return getPet(owner.getUniqueId());
    }

    public Optional<Pet> getPet(UUID ownerId) {
        Pet pet = petsByOwner.get(ownerId);
        if (pet == null) {
            return Optional.empty();
        }
        if (!pet.isAlive()) {
            petsByOwner.remove(ownerId);
            return Optional.empty();
        }
        return Optional.of(pet);
    }

    public Collection<Pet> activePets() {
        return petsByOwner.values();
    }

    /** Returns true if the given entity is a pet managed by this plugin. */
    public boolean isManagedPet(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING);
    }

    /** Returns the owner UUID stored on a pet entity, if any. */
    public Optional<UUID> getOwnerOf(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        String raw = entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public void rename(Pet pet, String name) {
        pet.getEntity().customName(Component.text(name, NamedTextColor.AQUA));
        pet.getEntity().setCustomNameVisible(true);
    }

    private void removeEntity(LivingEntity entity) {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    private void tickFollow() {
        petsByOwner.entrySet().removeIf(e -> !e.getValue().isAlive());

        for (Map.Entry<UUID, Pet> entry : petsByOwner.entrySet()) {
            Player owner = plugin.getServer().getPlayer(entry.getKey());
            Pet pet = entry.getValue();
            if (owner == null || !owner.isOnline()) {
                continue;
            }
            LivingEntity entity = pet.getEntity();
            if (!entity.getWorld().equals(owner.getWorld())) {
                entity.teleport(owner.getLocation());
                continue;
            }
            double distSq = entity.getLocation().distanceSquared(owner.getLocation());
            if (distSq > TELEPORT_DISTANCE_SQ) {
                entity.teleport(owner.getLocation());
            } else if (distSq > FOLLOW_DISTANCE_SQ && entity instanceof Mob mob) {
                mob.getPathfinder().moveTo(owner.getLocation(), FOLLOW_SPEED);
            }
        }
    }
}
