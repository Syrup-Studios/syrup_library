package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigSchemaNode;
import net.syrupstudios.syruplibrary.config.ConfigScreenElement;
import net.syrupstudios.syruplibrary.config.ConfigGroup;
import net.syrupstudios.syruplibrary.config.ConfigEditPolicy;
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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;

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
    private String selectedGroupId;
    private String presentationSignature;
    private boolean rebuildQueued;
    private boolean initialized;

    private ConfigSectionScreen(Screen parent, ConfigEditSession session, ConfigSchemaNode section,
                                Component initialStatus) {
        super(ConfigText.configTitle(session.config()));
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
        if (!initialized) {
            this.statusMessage = initialStatus;
            this.conflict = false;
            this.initialized = true;
        }

        List<String> groups = availableGroups();
        if (selectedGroupId == null || !groups.contains(selectedGroupId)) {
            selectedGroupId = groups.isEmpty() ? "" : groups.get(0);
        }
        boolean showGroups = groups.size() > 1;
        int listTop = showGroups ? 58 : 32;
        int listBottom = this.height - 48;
        this.list = new ConfigEntryList(this, this.width, listBottom - listTop, listTop, listBottom);
        list.rebuild(visibleElements(selectedGroupId));
        addRenderableWidget(list);

        if (showGroups) addGroupButtons(groups);

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
        presentationSignature = presentationSignature();
        rebuildQueued = false;
        updateButtons();
    }

    /** Called by an editor after one value changes. */
    void markChanged(ConfigValue<?> value) {
        String signature = presentationSignature();
        if (!Objects.equals(signature, presentationSignature) && !rebuildQueued) {
            presentationSignature = signature;
            rebuildQueued = true;
            minecraft.execute(this::rebuildWidgets);
        } else if (list != null) {
            list.refresh();
        }
        updateButtons();
    }

    private void addGroupButtons(List<String> groups) {
        if (groups.size() > 4) {
            String id = selectedGroupId;
            Component label = id.isEmpty() ? Component.translatable("syrup_library.config.default_group")
                    : ConfigText.groupName(session.config().spec(), group(id));
            addRenderableWidget(Button.builder(label, ignored -> {
                int index = groups.indexOf(selectedGroupId);
                selectGroup(groups.get((index + 1) % groups.size()));
            }).bounds(width / 2 - 100, 32, 200, 20).build());
            return;
        }
        int gap = 4;
        int availableWidth = Math.max(100, width - 40);
        int buttonWidth = Math.max(60, Math.min(120,
                (availableWidth - gap * (groups.size() - 1)) / groups.size()));
        int totalWidth = buttonWidth * groups.size() + gap * (groups.size() - 1);
        int x = (width - totalWidth) / 2;
        for (int index = 0; index < groups.size(); index++) {
            String id = groups.get(index);
            Component label = id.isEmpty() ? Component.translatable("syrup_library.config.default_group")
                    : ConfigText.groupName(session.config().spec(), group(id));
            Button button = addRenderableWidget(Button.builder(label, ignored -> selectGroup(id))
                    .bounds(x + index * (buttonWidth + gap), 32, buttonWidth, 20).build());
            button.active = !id.equals(selectedGroupId);
            if (!id.isEmpty()) {
                Component description = ConfigText.groupDescription(session.config().spec(), group(id));
                if (!description.getString().isEmpty()) button.setTooltip(Tooltip.create(description));
            }
        }
    }

    private ConfigGroup group(String id) {
        return section.groups().stream().filter(group -> group.id().equals(id)).findFirst().orElseThrow();
    }

    private void selectGroup(String id) {
        selectedGroupId = id;
        rebuildWidgets();
    }

    private List<String> availableGroups() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (ConfigScreenElement element : section.screenElements()) {
            if (!isVisible(element)) continue;
            String id = element.presentation().group().map(ConfigGroup::id).orElse("");
            if (id.isEmpty() || session.matches(element.presentation().group().orElseThrow().visibilityCondition())) {
                result.add(id);
            }
        }
        return List.copyOf(result);
    }

    private List<ConfigScreenElement> visibleElements(String groupId) {
        List<ConfigScreenElement> result = new ArrayList<>();
        for (ConfigScreenElement element : section.screenElements()) {
            if (!isVisible(element)) continue;
            String id = element.presentation().group().map(ConfigGroup::id).orElse("");
            if (id.equals(groupId)) result.add(element);
        }
        return result;
    }

    private boolean isVisible(ConfigScreenElement element) {
        if (element.presentation().editPolicy() == ConfigEditPolicy.HIDDEN) return false;
        if (!session.matches(element.presentation().visibilityCondition())) return false;
        return element.presentation().group()
                .map(group -> session.matches(group.visibilityCondition()))
                .orElse(true);
    }

    private String presentationSignature() {
        StringBuilder result = new StringBuilder();
        for (String group : availableGroups()) result.append('[').append(group).append(']');
        for (ConfigScreenElement element : section.screenElements()) {
            result.append(isVisible(element) ? '1' : '0');
        }
        return result.toString();
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
        List<ConfigValue<?>> editable = new ArrayList<>();
        collectEditable(session.config().spec().schema(), editable);
        session.resetValues(editable);
        list.refresh();
        statusMessage = null;
        updateButtons();
    }

    private void collectEditable(ConfigSchemaNode parent, List<ConfigValue<?>> output) {
        for (ConfigScreenElement element : parent.screenElements()) {
            if (!(element instanceof ConfigSchemaNode node) || !isGloballyVisible(node)) continue;
            if (node.isSection()) {
                collectEditable(node, output);
            } else if (node.presentation().editPolicy() == ConfigEditPolicy.EDITABLE
                    && session.matches(node.presentation().enabledCondition())) {
                output.add(node.value());
            }
        }
    }

    private boolean isGloballyVisible(ConfigSchemaNode node) {
        return node.presentation().editPolicy() != ConfigEditPolicy.HIDDEN
                && session.matches(node.presentation().visibilityCondition())
                && node.presentation().group().map(group -> session.matches(group.visibilityCondition())).orElse(true);
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
            return ConfigText.configTitle(session.config());
        }
        return ConfigText.sectionName(section);
    }

    private Component subtitle() {
        return Component.translatable(
                "syrup_library.config.file_path", session.config().path().getFileName().toString());
    }
}
