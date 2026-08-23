package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.syrupstudios.syruplibrary.config.ConfigInfoRow;
import net.syrupstudios.syruplibrary.config.ConfigScreenElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/** Scrollable list of config entries for one section. */
final class ConfigEntryList extends ContainerObjectSelectionList<ConfigListEntry> {
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

    void addElement(ConfigScreenElement element) {
        if (element instanceof ConfigSchemaNode node) addEntry(new ConfigEntryWidget(screen, node));
        else addEntry(new ConfigInfoWidget(screen, (ConfigInfoRow) element));
    }

    void rebuild(java.util.List<ConfigScreenElement> elements) {
        clearEntries();
        for (ConfigScreenElement element : elements) addElement(element);
    }

    void refresh() {
        for (ConfigListEntry widget : children()) widget.refresh();
    }

    java.util.List<net.syrupstudios.syruplibrary.config.value.ConfigValue<?>> editableValues() {
        java.util.List<net.syrupstudios.syruplibrary.config.value.ConfigValue<?>> values = new java.util.ArrayList<>();
        for (ConfigListEntry entry : children()) if (entry.isEditable()) values.add(entry.value());
        return values;
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
