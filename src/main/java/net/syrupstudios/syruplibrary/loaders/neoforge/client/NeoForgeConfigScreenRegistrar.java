package net.syrupstudios.syruplibrary.loaders.neoforge.client;

//? if neoforge {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;

/**
 * Attaches config screens to every NeoForge mod that owns a Syrup Library config.
 */
@EventBusSubscriber(modid = SyrupLibrary.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeConfigScreenRegistrar {
    private NeoForgeConfigScreenRegistrar() {
    }

    /** Registers factories after every mod has had an opportunity to register its configs. */
    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SyrupConfigManager.getInstance().registeredOwnerIds().forEach(ownerModId ->
                ModList.get().getModContainerById(ownerModId).ifPresent(container -> attach(container, ownerModId))));
    }

    private static void attach(ModContainer container, String ownerModId) {
        java.util.function.Supplier<IConfigScreenFactory> factory =
                () -> (modContainer, parent) -> SyrupConfigScreen.create(parent, ownerModId);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
//?}
