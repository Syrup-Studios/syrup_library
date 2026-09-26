package net.syrupstudios.syruplibrary.loaders.fabric;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.command.SyrupCommands;

/** Fabric entrypoint for Syrup Library. */
public final class FabricSyrupLibrary implements ModInitializer {
    @Override
    public void onInitialize() {
        SyrupLibrary.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                SyrupCommands.dispatch(dispatcher));
    }
}
//?}
