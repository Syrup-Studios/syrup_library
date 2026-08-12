package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/** Scrollable list of config entries for one section. */
final class ConfigEntryList extends ContainerObjectSelectionList<ConfigEntryWidget> {
    static final int ENTRY_HEIGHT = 32;

    private final ConfigSectionScreen screen;

    ConfigEntryList(ConfigSectionScreen screen, int width, int height, int y0, int y1) {
        //? if >=1.21.11 {
        /*super(Minecraft.getInstance(), width, height, y0, ENTRY_HEIGHT);
        updateSizeAndPosition(width, height, 0, y0);
         *///?} elif >=1.21 {
        /*super(Minecraft.getInstance(), width, height, y0, ENTRY_HEIGHT);
        updateSizeAndPosition(width, height, y0);
         *///?} else {
        super(Minecraft.getInstance(), width, height, y0, y1, ENTRY_HEIGHT);
        //?}
        this.screen = screen;
    }

    void addEntry(ConfigSchemaNode entry) {
        addEntry(new ConfigEntryWidget(screen, entry));
    }

    void refresh() {
        for (ConfigEntryWidget widget : children()) {
            widget.refresh();
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(600, Math.max(160, this.width - 40));
    }

    //? if <1.21.11 {
    @Override
    protected int getScrollbarPosition() {
        return this.width / 2 + getRowWidth() / 2 + 6;
    }
    //?}
}
