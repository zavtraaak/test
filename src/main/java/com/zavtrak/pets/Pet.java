package com.zavtrak.pets;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Runtime handle for an active pet: the live entity plus its owner.
 *
 * <p>Stored in memory by {@link PetManager}. Persistence across restarts is
 * handled via the entity's {@link org.bukkit.persistence.PersistentDataContainer}
 * so that the pet can be re-bound to its owner if the chunk reloads.</p>
 */
public final class Pet {
    private final UUID ownerId;
    private final LivingEntity entity;
    private final PetType type;

    public Pet(UUID ownerId, LivingEntity entity, PetType type) {
        this.ownerId = ownerId;
        this.entity = entity;
        this.type = type;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public PetType getType() {
        return type;
    }

    public boolean isAlive() {
        return entity != null && entity.isValid() && !entity.isDead();
    }
}
