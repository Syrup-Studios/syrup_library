package net.syrupstudios.syruplibrary.loaders.fabric.client;

//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.syrupstudios.syruplibrary.client.config.SyrupConfigScreen;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import net.minecraft.client.gui.screens.Screen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional Fabric Mod Menu integration.
 *
 * <p>Syrup Library provides config screens for every mod that owns registered configs, so Mod Menu
 * shows a config button for those mods without them depending on Syrup Library's UI directly.
 */
public final class FabricModMenuIntegration implements ModMenuApi {
    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
        for (String ownerModId : SyrupConfigManager.getInstance().registeredOwnerIds()) {
            factories.put(ownerModId, (ConfigScreenFactory<Screen>) parent ->
                    SyrupConfigScreen.create(parent, ownerModId));
        }
        return factories;
    }
}
//?}
