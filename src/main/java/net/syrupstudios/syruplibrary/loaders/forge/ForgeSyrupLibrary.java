package net.syrupstudios.syruplibrary.loaders.forge;

//? if forge {
/*import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.command.SyrupCommands;
import net.syrupstudios.syruplibrary.loaders.forge.ForgePlatformImpl;

/^* Forge entrypoint for Syrup Library. ^/
@Mod(SyrupLibrary.MOD_ID)
public final class ForgeSyrupLibrary {
    public ForgeSyrupLibrary() {
        SyrupLibrary.initialize();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(ForgePlatformImpl::registerPermissionNodes);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        SyrupCommands.dispatch(event.getDispatcher());
    }
}
*///?}
