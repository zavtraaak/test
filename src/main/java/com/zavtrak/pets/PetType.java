package com.zavtrak.pets;

import org.bukkit.entity.EntityType;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalogue of pet types players can summon.
 *
 * <p>Each type maps a friendly command name to a Bukkit {@link EntityType}.
 * Only mobs that are reasonable to keep as a companion are exposed here.</p>
 */
public enum PetType {
    WOLF(EntityType.WOLF),
    CAT(EntityType.CAT),
    OCELOT(EntityType.OCELOT),
    PARROT(EntityType.PARROT),
    FOX(EntityType.FOX),
    AXOLOTL(EntityType.AXOLOTL),
    FROG(EntityType.FROG),
    BEE(EntityType.BEE),
    PANDA(EntityType.PANDA),
    RABBIT(EntityType.RABBIT),
    CHICKEN(EntityType.CHICKEN),
    PIG(EntityType.PIG);

    private final EntityType entityType;

    PetType(EntityType entityType) {
        this.entityType = entityType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String displayName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<PetType> match(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.name().equals(normalized))
                .findFirst();
    }
}
