package net.syrupstudios.syruplibrary.teleport;

import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
//? if >=1.21.11 {
import net.minecraft.world.entity.Relative;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Stateless helpers for capturing and moving server players. */
public final class SyrupTeleports {
    private SyrupTeleports() {}

    /** Captures the player's current dimension, position, and rotation on the server thread. */
    public static TeleportTarget capture(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ServerLevel level = (ServerLevel) player.level();
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Player locations must be captured on the server thread");
        }
        return new TeleportTarget(level.dimension(), player.position(), player.getYRot(), player.getXRot());
    }

    /** Teleports on the server thread. Yaw is wrapped and pitch is clamped to vanilla limits. */
    public static TeleportResult teleport(ServerPlayer player, TeleportTarget target) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");

        ServerLevel source = (ServerLevel) player.level();
        if (!source.getServer().isSameThread()) {
            return TeleportResult.WRONG_THREAD;
        }

        Vec3 position = target.position();
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)
                || !Float.isFinite(target.yaw()) || !Float.isFinite(target.pitch())
                || Math.abs(position.x) > 30_000_000 || Math.abs(position.y) > 20_000_000
                || Math.abs(position.z) > 30_000_000
                || !Level.isInSpawnableBounds(BlockPos.containing(position))) {
            return TeleportResult.INVALID_DESTINATION;
        }

        ServerLevel destination = source.getServer().getLevel(target.dimension());
        if (destination == null) {
            return TeleportResult.UNKNOWN_DIMENSION;
        }

        float yaw = Mth.wrapDegrees(target.yaw());
        float pitch = Mth.clamp(target.pitch(), -90.0F, 90.0F);
        boolean moved;
        //? if >=1.21.11 {
        moved = player.teleportTo(destination, position.x, position.y, position.z,
                Set.of(Relative.DELTA_X, Relative.DELTA_Y, Relative.DELTA_Z),
                yaw, pitch, true);
        //?} else {
        /*moved = player.teleportTo(destination, position.x, position.y, position.z, Set.of(),
                yaw, pitch);
        *///?}
        return moved && player.level() == destination && player.position().equals(position)
                && player.getYRot() == yaw && player.getXRot() == pitch
                ? TeleportResult.SUCCESS
                : TeleportResult.TELEPORT_REJECTED;
    }
}
