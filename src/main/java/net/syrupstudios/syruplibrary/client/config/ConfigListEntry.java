package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/** Common list-entry type for stored values, sections, and information rows. */
abstract class ConfigListEntry extends ContainerObjectSelectionList.Entry<ConfigListEntry> {
    void refresh() {
    }

    boolean isEditable() { return false; }

    ConfigValue<?> value() { return null; }
}
