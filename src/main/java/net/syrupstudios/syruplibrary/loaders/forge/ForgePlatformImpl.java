package net.syrupstudios.syruplibrary.loaders.forge;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import net.syrupstudios.syruplibrary.loaders.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

/^* Forge implementation of shared loader services. ^/
public final class ForgePlatformImpl implements Platform {
    private static final Map<String, PermissionNode<Boolean>> PERMISSION_NODES = new LinkedHashMap<>();

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isClientSide() {
        return FMLLoader.getDist() == Dist.CLIENT;
    }

    @Override
    public boolean isServerSide() {
        return FMLLoader.getDist() == Dist.DEDICATED_SERVER;
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get().toAbsolutePath().normalize();
    }

    @Override
    public String loader() {
        return "forge";
    }

    @Override
    public Predicate<CommandSourceStack> commandPermission(String modId, String node, boolean allowByDefault) {
        PermissionNode<Boolean> permission = PERMISSION_NODES.computeIfAbsent(modId + "." + node, key ->
                new PermissionNode<>(modId, node, PermissionTypes.BOOLEAN,
                        (player, uuid, context) -> allowByDefault
                                || player != null && player.hasPermissions(Commands.LEVEL_GAMEMASTERS)));
        return source -> source.getEntity() instanceof ServerPlayer player
                ? PermissionAPI.getPermission(player, permission)
                : allowByDefault || source.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }

    static Collection<PermissionNode<?>> permissionNodes() {
        return new ArrayList<>(PERMISSION_NODES.values());
    }

    public static void registerPermissionNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(permissionNodes());
    }
}
*///?}
