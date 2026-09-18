package net.syrupstudios.syruplibrary.loaders.fabric.client;

//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;

import java.util.Map;
import java.util.stream.Collectors;

/** Optional ModMenu bridge. Each factory filters to its owning mod at screen creation time. */
public final class FabricModMenuIntegration implements ModMenuApi {
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getId())
                .collect(Collectors.toUnmodifiableMap(
                        id -> id, id -> parent -> SyrupConfigScreen.createForMod(parent, id)));
    }
}
//?}
