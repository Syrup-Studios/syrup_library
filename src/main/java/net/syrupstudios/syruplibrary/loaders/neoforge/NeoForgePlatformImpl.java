package net.syrupstudios.syruplibrary.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import net.syrupstudios.syruplibrary.loaders.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.function.Predicate;

/^* NeoForge implementation of shared loader services. ^/
public final class NeoForgePlatformImpl implements Platform {
    private static final Map<String, PermissionNode<Boolean>> PERMISSION_NODES = new LinkedHashMap<>();

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isClientSide() {
        //? if >=1.21.11 {
        /^return FMLEnvironment.getDist() == Dist.CLIENT;
        ^///?} else
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public boolean isServerSide() {
        //? if >=1.21.11 {
        /^return FMLEnvironment.getDist() == Dist.DEDICATED_SERVER;
        ^///?} else
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get().toAbsolutePath().normalize();
    }

    @Override
    public String loader() {
        return "neoforge";
    }

    @Override
    public Predicate<CommandSourceStack> commandPermission(String modId, String node, boolean allowByDefault) {
        PermissionNode<Boolean> permission = PERMISSION_NODES.computeIfAbsent(modId + "." + node, key ->
                new PermissionNode<>(modId, node, PermissionTypes.BOOLEAN,
                        (player, uuid, context) -> allowByDefault || vanillaFallback(player)
                ));
        return source -> source.getEntity() instanceof ServerPlayer player
                ? PermissionAPI.getPermission(player, permission)
                : allowByDefault || vanillaFallback(source);
    }

    private static boolean vanillaFallback(ServerPlayer player) {
        //? if >=1.21.11 {
        /^return player != null && Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)
                .test(player.createCommandSourceStack());
        ^///?} else
        return player != null && player.hasPermissions(Commands.LEVEL_GAMEMASTERS);
    }

    private static boolean vanillaFallback(CommandSourceStack source) {
        //? if >=1.21.11 {
        /^return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
        ^///?} else
        return source.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }

    static Collection<PermissionNode<?>> permissionNodes() {
        return new ArrayList<>(PERMISSION_NODES.values());
    }

    public static void registerPermissionNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(permissionNodes());
    }
}
*///?}
