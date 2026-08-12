package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.syrupstudios.syruplibrary.config.ConfigSnapshot;
import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import net.syrupstudios.syruplibrary.config.edit.ConfigEditSession;
import net.syrupstudios.syruplibrary.config.edit.ConfigSaveResult;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** In-game editor for one config section. */
final class ConfigSectionScreen extends AbstractConfigScreen {
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_HINT = 0xFFA0A0A0;
    private static final int COLOR_ERROR = 0xFFFF5555;
    private static final int COLOR_NOTICE = 0xFFFFFF55;

    private final Screen parent;
    private final ConfigEditSession session;
    private final ConfigSchemaNode section;
    private final Component initialStatus;

    private ConfigEntryList list;
    private Button saveButton;
    private Button resetAllButton;
    private Button closeButton;
    private Button reloadButton;
    private Component statusMessage;
    private boolean conflict;

    private ConfigSectionScreen(Screen parent, ConfigEditSession session, ConfigSchemaNode section,
                                Component initialStatus) {
        super(Component.translatable("syrup_library.config.title"));
        this.parent = parent;
        this.session = session;
        this.section = section;
        this.initialStatus = initialStatus;
    }

    /** Opens an editor for a config, starting a fresh edit session. */
    static ConfigSectionScreen open(Screen parent, RegisteredConfig config, ConfigSchemaNode section) {
        return new ConfigSectionScreen(parent, ConfigEditSession.open(config), section, null);
    }

    /** Creates an editor around an existing session (used when navigating sections and reloading). */
    static ConfigSectionScreen withSession(Screen parent, ConfigEditSession session, ConfigSchemaNode section) {
        return new ConfigSectionScreen(parent, session, section, null);
    }

    /** Creates an editor around an existing session, showing an initial notice. */
    static ConfigSectionScreen withSession(Screen parent, ConfigEditSession session, ConfigSchemaNode section,
                                           Component initialStatus) {
        return new ConfigSectionScreen(parent, session, section, initialStatus);
    }

    ConfigEditSession session() {
        return session;
    }

    ConfigSchemaNode section() {
        return section;
    }

    Screen parent() {
        return parent;
    }

    boolean isRootSection() {
        return section.path().isEmpty();
    }

    @Override
    protected void init() {
        this.statusMessage = initialStatus;
        this.conflict = false;

        int listBottom = this.height - 48;
        this.list = new ConfigEntryList(this, this.width, listBottom - 32, 32, listBottom);
        for (ConfigSchemaNode entry : section.children()) {
            list.addEntry(entry);
        }
        addRenderableWidget(list);

        saveButton = addRenderableWidget(Button.builder(
                Component.translatable("syrup_library.config.save"), button -> save())
                .bounds(0, 0, 100, 20)
                .build());
        resetAllButton = addRenderableWidget(Button.builder(
                Component.translatable("syrup_library.config.reset_all"), button -> resetAll())
                .bounds(0, 0, 100, 20)
                .build());
        closeButton = addRenderableWidget(Button.builder(
                isRootSection()
                        ? Component.translatable("syrup_library.config.cancel")
                        : Component.translatable("syrup_library.config.back"),
                button -> onClose())
                .bounds(0, 0, 100, 20)
                .build());
        reloadButton = addRenderableWidget(Button.builder(
                Component.translatable("syrup_library.config.reload"), button -> reload())
                .bounds(0, 0, 100, 20)
                .build());
        reloadButton.visible = false;
        updateButtons();
    }

    /** Called by entries whenever a draft value changes. */
    void markChanged() {
        updateButtons();
    }

    private void updateButtons() {
        if (saveButton != null) {
            saveButton.active = session.isValid() && session.isDirty();
        }
        if (resetAllButton != null) {
            resetAllButton.active = true;
        }
        if (reloadButton != null) {
            reloadButton.visible = conflict;
        }
        layoutFooter();
    }

    private void layoutFooter() {
        if (saveButton == null || resetAllButton == null || closeButton == null || reloadButton == null) {
            return;
        }
        List<Button> buttons = new ArrayList<>(List.of(saveButton, resetAllButton, closeButton));
        if (conflict) buttons.add(reloadButton);
        int gap = 6;
        int buttonWidth = Math.max(40, Math.min(100,
                (this.width - 40 - gap * (buttons.size() - 1)) / buttons.size()));
        int startX = (this.width - buttonWidth * buttons.size() - gap * (buttons.size() - 1)) / 2;
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            button.setX(startX + index * (buttonWidth + gap));
            button.setY(this.height - 28);
            button.setWidth(buttonWidth);
        }
    }

    private void save() {
        statusMessage = null;
        if (!session.isValid()) {
            statusMessage = Component.translatable("syrup_library.config.invalid");
            return;
        }
        if (!session.isDirty()) {
            return;
        }
        boolean restartRequired = restartRequiredByDraft();
        ConfigSaveResult result = session.save();
        if (result.success()) {
            if (restartRequired) {
                Component notice = Component.translatable("syrup_library.config.restart_required");
                ConfigSectionScreen next = ConfigSectionScreen.withSession(
                        rootParent(),
                        ConfigEditSession.open(session.config()),
                        session.config().spec().schema(),
                        notice
                );
                openScreen(next);
            } else {
                openScreen(rootParent());
            }
        } else if (result.fileChanged()) {
            conflict = true;
            statusMessage = Component.translatable("syrup_library.config.file_changed");
            updateButtons();
        } else {
            statusMessage = Component.literal(result.message());
        }
    }

    private boolean restartRequiredByDraft() {
        ConfigSnapshot startup = session.config().startupSnapshot();
        for (java.util.Map.Entry<ConfigValue<?>, Object> entry : session.draftValues().entrySet()) {
            if (entry.getKey().restartRequirement() == RestartRequirement.REQUIRED
                    && !Objects.equals(entry.getValue(), startup.get(entry.getKey()))) {
                return true;
            }
        }
        return false;
    }

    private void resetAll() {
        session.resetAll();
        list.refresh();
        statusMessage = null;
        updateButtons();
    }

    private void reload() {
        ConfigEditSession reopened = ConfigEditSession.open(session.config());
        openScreen(ConfigSectionScreen.withSession(
                rootParent(), reopened, session.config().spec().schema()));
    }

    private Screen rootParent() {
        ConfigSectionScreen root = this;
        while (root.parent instanceof ConfigSectionScreen parentSection
                && parentSection.session == session) {
            root = parentSection;
        }
        return root.parent;
    }

    /** Back (nested section) or Cancel (root section). Back keeps drafts; root Cancel discards them. */
    @Override
    public void onClose() {
        openScreen(parent);
    }

    //? if >=26 {
    /*@Override
    protected void extractConfigContents(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, sectionTitle(), this.width / 2, 8, COLOR_TITLE);
        drawCenteredText(graphics, subtitle(), this.width / 2, 22, COLOR_HINT);
        if (statusMessage != null) {
            int color = statusMessage.getStyle().getColor() == null
                    ? (conflict ? COLOR_ERROR : COLOR_NOTICE)
                    : statusMessage.getStyle().getColor().getValue();
            drawCenteredText(graphics, statusMessage, this.width / 2, this.height - 38, color);
        }
    }
     *///?} else {
    @Override
    protected void renderConfigContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, sectionTitle(), this.width / 2, 8, COLOR_TITLE);
        drawCenteredText(graphics, subtitle(), this.width / 2, 22, COLOR_HINT);
        if (statusMessage != null) {
            int color = statusMessage.getStyle().getColor() == null
                    ? (conflict ? COLOR_ERROR : COLOR_NOTICE)
                    : statusMessage.getStyle().getColor().getValue();
            drawCenteredText(graphics, statusMessage, this.width / 2, this.height - 38, color);
        }
    }
    //?}

    private Component sectionTitle() {
        if (section.path().isEmpty()) {
            return Component.literal(ConfigText.displayName(session.config().spec().id()));
        }
        return ConfigText.sectionName(section);
    }

    private Component subtitle() {
        return Component.translatable(
                "syrup_library.config.file_path", session.config().path().getFileName().toString());
    }
}
