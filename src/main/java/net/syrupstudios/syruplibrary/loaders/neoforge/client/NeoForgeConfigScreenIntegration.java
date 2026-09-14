package net.syrupstudios.syruplibrary.loaders.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;

/** Registers the NeoForge config screen only in a client environment. */
//? if >=1.21.11 {
/*@EventBusSubscriber(modid = SyrupLibrary.MOD_ID, value = Dist.CLIENT)
*///?} else
@EventBusSubscriber(modid = SyrupLibrary.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NeoForgeConfigScreenIntegration {
    private NeoForgeConfigScreenIntegration() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (container, parent) -> SyrupConfigScreen.create(parent));
    }
}
