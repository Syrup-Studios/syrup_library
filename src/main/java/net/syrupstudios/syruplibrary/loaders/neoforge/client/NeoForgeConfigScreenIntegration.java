package net.syrupstudios.syruplibrary.loaders.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;

/** Registers a filtered config screen for the owning mod after client setup. */
//? if >=1.21.11 {
@EventBusSubscriber(modid = SyrupLibrary.MOD_ID, value = Dist.CLIENT)
//?} else
//@EventBusSubscriber(modid = SyrupLibrary.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NeoForgeConfigScreenIntegration {
    private NeoForgeConfigScreenIntegration() {}

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> SyrupConfigManager.getInstance().registeredConfigs().values().stream()
                .map(config -> config.spec().ownerId()).distinct()
                .forEach(owner -> ModList.get().getModContainerById(owner).ifPresent(container -> {
                    if (container.getCustomExtension(IConfigScreenFactory.class).isEmpty()) {
                        java.util.function.Supplier<IConfigScreenFactory> factory =
                                () -> (modContainer, parent) -> SyrupConfigScreen.createForMod(parent, owner);
                        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
                    }
                })));
    }
}
