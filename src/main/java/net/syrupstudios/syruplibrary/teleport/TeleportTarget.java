package net.syrupstudios.syruplibrary.teleport;

import java.util.Objects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A player's destination, without persistence or command state. */
public record TeleportTarget(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {
    public TeleportTarget {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
    }
}
