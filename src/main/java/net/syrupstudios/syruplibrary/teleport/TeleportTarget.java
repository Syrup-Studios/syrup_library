package net.syrupstudios.syruplibrary.teleport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** An immutable teleport destination without command state. */
public record TeleportTarget(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {
    public static final Codec<TeleportTarget> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(TeleportTarget::dimension),
            Vec3.CODEC.fieldOf("pos").forGetter(TeleportTarget::position),
            Codec.FLOAT.fieldOf("yaw").forGetter(TeleportTarget::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(TeleportTarget::pitch)
    ).apply(builder, TeleportTarget::new));

    public TeleportTarget {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
    }
}
