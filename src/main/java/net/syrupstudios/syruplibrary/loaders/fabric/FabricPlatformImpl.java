package net.syrupstudios.syruplibrary.loaders.fabric;

//? if fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
//? if >=26.2
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
//? if >=26.2
import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
//? if >=26.2
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
//? if <=1.21.11 {
/*import me.lucko.fabric.api.permissions.v0.Permissions;
*///?}
//? if >=26.2
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.syrupstudios.syruplibrary.loaders.Platform;

import java.nio.file.Path;
import java.util.function.Predicate;

/** Fabric implementation of shared loader services. */
public final class FabricPlatformImpl implements Platform {
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isClientSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isServerSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir().toAbsolutePath().normalize();
    }

    @Override
    public String loader() {
        return "fabric";
    }

    @Override
    public Predicate<CommandSourceStack> commandPermission(String modId, String node, boolean allowByDefault) {
        //? if >=26.2 {
        if (!FabricLoader.getInstance().isModLoaded("fabric-permission-api-v1")) {
            return vanillaFallback(allowByDefault);
        }
        PermissionNode<Boolean> permission = PermissionNode.of(modId, node);
        Predicate<PermissionContextOwner> predicate = PermissionPredicates.require(permission,
                allowByDefault ? PermissionLevel.ALL : PermissionLevel.GAMEMASTERS);
        return source -> predicate.test((PermissionContextOwner) (Object) source);
        //?} else {
        /*if (FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")) {
            return luckoPermission(modId + "." + node, allowByDefault);
        }
        return vanillaFallback(allowByDefault);
        *///?}
    }

    //? if <=1.21.11 {
    /*private static Predicate<CommandSourceStack> luckoPermission(String node, boolean allowByDefault) {
        return Permissions.require(node, allowByDefault ? 0 : 2);
    }
    *///?}

    private static Predicate<CommandSourceStack> vanillaFallback(boolean allowByDefault) {
        //? if >=1.21.11 {
        return source -> allowByDefault || Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
        //?} else {
        /*return source -> allowByDefault || source.hasPermission(Commands.LEVEL_GAMEMASTERS);
        *///?}
    }
}
//?}
