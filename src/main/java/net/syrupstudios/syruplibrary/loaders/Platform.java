package net.syrupstudios.syruplibrary.loaders;

import net.minecraft.commands.CommandSourceStack;

import java.nio.file.Path;
import java.util.function.Predicate;

/** Loader-specific services used by shared Syrup Library code. */
public interface Platform {
    //? if fabric
    Platform INSTANCE = new net.syrupstudios.syruplibrary.loaders.fabric.FabricPlatformImpl();
    //? if forge
    /*Platform INSTANCE = new net.syrupstudios.syruplibrary.loaders.forge.ForgePlatformImpl();*/
    //? if neoforge
    /*Platform INSTANCE = new net.syrupstudios.syruplibrary.loaders.neoforge.NeoForgePlatformImpl();*/

    /** Returns whether a mod with the supplied ID is loaded. */
    boolean isModLoaded(String modId);

    /** Returns whether the game is running in a client environment. */
    boolean isClientSide();

    /** Returns whether the game is running in a dedicated-server environment. */
    boolean isServerSide();

    /** Returns the normalized directory in which mod configuration files are stored. */
    Path configDirectory();

    /** Returns the lowercase loader identifier. */
    String loader();

    /** Creates a command permission predicate with the supplied vanilla fallback. */
    Predicate<CommandSourceStack> commandPermission(String modId, String node, boolean allowByDefault);
}
