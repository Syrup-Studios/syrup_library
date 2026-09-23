package net.syrupstudios.syruplibrary.client.config;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.syrupstudios.syruplibrary.config.RegisteredConfig;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Paged selection of the supplied registered configs. */
final class SyrupConfigSelectionScreen extends ConfigScreen {
    private int page;

    private final List<RegisteredConfig> configs;

    SyrupConfigSelectionScreen(Screen parent, Collection<RegisteredConfig> configs, String title) {
        super(parent, Component.literal(title));
        this.configs = configs.stream().sorted(Comparator.comparing(config -> config.spec().id())).toList();
    }

    @Override protected void init() {
        int rows = Math.max(1, (height - 110) / 24);
        int pages = Math.max(1, (configs.size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        label(title.getString() + "  " + (page + 1) + "/" + pages, 12);
        if (configs.isEmpty()) label("No configs are registered.", 40);
        for (int i = page * rows; i < Math.min(configs.size(), (page + 1) * rows); i++) {
            RegisteredConfig config = configs.get(i);
            button(config.spec().id(), left(), 36 + (i % rows) * 24, contentWidth(),
                    () -> show(new SyrupConfigScreen(this, config)));
        }
        button("Previous", left(), height - 52, 90,
                () -> { page--; rebuildWidgets(); }).active = page > 0;
        button("Next", left() + contentWidth() - 90, height - 52, 90,
                () -> { page++; rebuildWidgets(); }).active = page + 1 < pages;
        button("Back", left(), height - 28, contentWidth(), this::onClose);
    }
}
