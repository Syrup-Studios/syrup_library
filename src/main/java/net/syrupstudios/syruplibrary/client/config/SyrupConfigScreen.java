package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;

/**
 * Entry point for opening the config editor for a mod's registered configs.
 */
public final class SyrupConfigScreen {
    private SyrupConfigScreen() {
    }

    /**
     * Creates the appropriate screen for {@code ownerModId}, or returns {@code null} when the mod
     * owns no configs.
     *
     * <p>A mod with a single config opens the editor directly; multiple configs open a file
     * selection page first.
     */
    public static Screen create(Screen parent, String ownerModId) {
        Map<String, RegisteredConfig> configs =
                SyrupConfigManager.getInstance().registeredConfigs(ownerModId);
        if (configs.isEmpty()) {
            return null;
        }
        if (configs.size() == 1) {
            RegisteredConfig config = configs.values().iterator().next();
            return ConfigSectionScreen.open(parent, config, config.spec().schema());
        }
        return new ConfigFileSelectionScreen(parent, ownerModId);
    }
}
