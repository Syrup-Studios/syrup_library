package net.syrupstudios.syruplibrary.client.config;

//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base screen for the Syrup Library config editor.
 *
 * <p>Rendering moved from {@code GuiGraphics} to {@code GuiGraphicsExtractor} in Minecraft 26;
 * subclasses override the matching contents hook for their version.
 */
abstract class AbstractConfigScreen extends Screen {
    protected AbstractConfigScreen(Component title) {
        super(title);
    }

    //? if >=26 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractConfigContents(graphics, mouseX, mouseY, partialTick);
    }

    protected void extractConfigContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }
     *///?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //? if >=1.21 {
        /*renderBackground(graphics, mouseX, mouseY, partialTick);
         *///?} else {
        renderBackground(graphics);
        //?}
        super.render(graphics, mouseX, mouseY, partialTick);
        renderConfigContents(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderConfigContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
    //?}

    //? if >=26 {
    /*protected void drawCenteredText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.centeredText(font, text, x, y, color);
    }
     *///?} else {
    protected void drawCenteredText(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawCenteredString(font, text, x, y, color);
    }
    //?}

    /** Switches the client to the supplied screen, matching the version's API. */
    void openScreen(Screen next) {
        //? if >=26 {
        /*minecraft.setScreenAndShow(next);
         *///?} else {
        minecraft.setScreen(next);
        //?}
    }

    /** Package-level access to the client instance for entry widgets. */
    net.minecraft.client.Minecraft minecraftInstance() {
        return minecraft;
    }

    /** Package-level access to the font for entry widgets. */
    net.minecraft.client.gui.Font fontInstance() {
        return font;
    }
}
