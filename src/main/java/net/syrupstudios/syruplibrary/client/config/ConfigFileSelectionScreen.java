package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;

/** Lets a player pick which config file of a mod to edit. */
final class ConfigFileSelectionScreen extends AbstractConfigScreen {
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_HINT = 0xFFA0A0A0;

    private final Screen parent;
    private final Map<String, RegisteredConfig> configs;

    ConfigFileSelectionScreen(Screen parent, String ownerModId) {
        super(Component.translatable("syrup_library.config.file_selection"));
        this.parent = parent;
        this.configs = SyrupConfigManager.getInstance().registeredConfigs(ownerModId);
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(300, this.width - 40);
        int x = (this.width - buttonWidth) / 2;
        int y = 44;
        for (Map.Entry<String, RegisteredConfig> entry : configs.entrySet()) {
            String configId = entry.getKey();
            RegisteredConfig config = entry.getValue();
            addRenderableWidget(Button.builder(Component.literal(ConfigText.displayName(configId)), button -> openEditor(config))
                    .bounds(x, y, buttonWidth, 20)
                    .build());
            y += 26;
        }
    }

    private void openEditor(RegisteredConfig config) {
        openScreen(ConfigSectionScreen.open(this, config, config.spec().schema()));
    }

    @Override
    public void onClose() {
        openScreen(parent);
    }

    //? if >=26 {
    /*@Override
    protected void extractConfigContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, Component.translatable("syrup_library.config.file_selection"),
                this.width / 2, 8, COLOR_TITLE);
        drawCenteredText(graphics, Component.translatable("syrup_library.config.select_file"),
                this.width / 2, 22, COLOR_HINT);
    }
     *///?} else {
    @Override
    protected void renderConfigContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, Component.translatable("syrup_library.config.file_selection"),
                this.width / 2, 8, COLOR_TITLE);
        drawCenteredText(graphics, Component.translatable("syrup_library.config.select_file"),
                this.width / 2, 22, COLOR_HINT);
    }
    //?}
}
