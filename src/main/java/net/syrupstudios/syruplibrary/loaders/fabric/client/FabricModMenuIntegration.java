package net.syrupstudios.syruplibrary.loaders.fabric.client;

//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;

/** Optional ModMenu bridge. ModMenu is compile-only and is never packaged. */
public final class FabricModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SyrupConfigScreen::create;
    }
}
//?}
