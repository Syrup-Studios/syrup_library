package net.syrupstudios.syruplibrary.loaders.neoforge;

//? if neoforge {
/*import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.command.SyrupCommands;
import net.syrupstudios.syruplibrary.loaders.neoforge.NeoForgePlatformImpl;

/^* NeoForge entrypoint for Syrup Library. ^/
@Mod(SyrupLibrary.MOD_ID)
public final class NeoForgeSyrupLibrary {
    public NeoForgeSyrupLibrary() {
        SyrupLibrary.initialize();
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(NeoForgePlatformImpl::registerPermissionNodes);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        SyrupCommands.dispatch(event.getDispatcher());
    }
}
*///?}
