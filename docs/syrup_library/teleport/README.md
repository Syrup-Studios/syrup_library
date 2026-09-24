---
title: Teleport players
description: Capture player locations and teleport players with Syrup Library.
---

# Teleport players

Call the teleport API on the server thread. `TeleportTarget` stores a dimension,
position, yaw, and pitch. The coordinates below are a fixed example chosen by
the caller.

Use `TeleportTarget.CODEC` to persist targets in a mod's saved data. It uses the
fields `dim`, `pos`, `yaw`, and `pitch`.

```java
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.syrupstudios.syruplibrary.teleport.SyrupTeleports;
import net.syrupstudios.syruplibrary.teleport.TeleportResult;
import net.syrupstudios.syruplibrary.teleport.TeleportTarget;

public void teleportToLobby(ServerPlayer player) {
    TeleportTarget destination = new TeleportTarget(
        Level.OVERWORLD,
        new Vec3(0.5, 80, 0.5),
        0,
        0
    );

    TeleportResult result = SyrupTeleports.teleport(player, destination);
    if (result != TeleportResult.SUCCESS) {
        player.sendSystemMessage(Component.literal("Could not teleport you to the lobby."));
    }
}
```

## Server thread

Call `capture` on the server thread. It throws `IllegalStateException` if called
from another thread.

## Results

`teleport` returns a `TeleportResult`. It reports invalid coordinates, a missing
dimension, a call from the wrong thread, or a teleport that the server did not
complete.

## Destination behavior

The example builds a destination from coordinates chosen by the caller. A mod
can also load a saved home or configured location, or use `SyrupTeleports.capture`
to record a player's current location. Yaw is wrapped and pitch is clamped to
vanilla limits. The API does not search for a safe position. The player's motion
is preserved across dimensions. Store homes, warps, history, and request state
in the calling mod.
