package net.syrupstudios.syruplibrary.loaders.forge.client;

//? if forge {
/*import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;

 /^* Registers a filtered config screen for the owning mod after client setup. ^/
@Mod.EventBusSubscriber(modid = SyrupLibrary.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeConfigScreenIntegration {
    private ForgeConfigScreenIntegration() {}

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> SyrupConfigManager.getInstance().registeredConfigs().values().stream()
                .map(config -> config.spec().ownerId()).distinct()
                .forEach(owner -> ModList.get().getModContainerById(owner).ifPresent(container -> {
                    if (container.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).isEmpty()) {
                        container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                                () -> new ConfigScreenHandler.ConfigScreenFactory(
                                        (parent) -> SyrupConfigScreen.createForMod(parent, owner)));
                    }
                })));
    }

}
*///?}
