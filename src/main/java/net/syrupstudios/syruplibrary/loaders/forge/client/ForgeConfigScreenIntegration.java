package net.syrupstudios.syruplibrary.loaders.forge.client;

//? if forge {
/*import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;

 /^* Registers the Forge config screen only in a client environment. ^/
@Mod.EventBusSubscriber(modid = SyrupLibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeConfigScreenIntegration {
    private ForgeConfigScreenIntegration() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(ForgeConfigScreenIntegration::create));
    }

    private static Screen create(Screen parent) {
        return SyrupConfigScreen.create(parent);
    }
}
*///?}
