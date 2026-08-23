package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.value.StringConfigValue;
//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;

/** Dedicated multiline string editor with scrolling and exact newline preservation. */
final class MultilineConfigEditorScreen extends AbstractConfigScreen {
    private final ConfigEditorContext context;
    private final StringConfigValue definition;
    private MultiLineEditBox textBox;
    private Button done;
    private Component error;

    MultilineConfigEditorScreen(ConfigEditorContext context) {
        super(Component.translatable("syrup_library.config.multiline_editor"));
        this.context = context;
        this.definition = (StringConfigValue) context.valueDefinition();
    }

    @Override protected void init() {
        int center = width / 2;
        int requestedLines = context.node().presentation().editor().visibleLines();
        int boxHeight = Math.max(60, Math.min(height - 96, requestedLines * 12 + 8));
        //? if >=1.21.11 {
        /*textBox = MultiLineEditBox.builder().setX(center - 150).setY(32)
                .build(font, 300, boxHeight, Component.empty());
         *///?} else {
        textBox = new MultiLineEditBox(font, center - 150, 32, 300, boxHeight,
                Component.empty(), Component.empty());
        //?}
        textBox.setCharacterLimit(Integer.MAX_VALUE);
        textBox.setValue(String.valueOf(context.value()));
        textBox.setValueListener(ignored -> validateValue());
        addRenderableWidget(textBox);
        addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.cancel"), button -> closeToParent())
                .bounds(center - 154, height - 28, 150, 20).build());
        done = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.done"), button -> save())
                .bounds(center + 4, height - 28, 150, 20).build());
        validateValue();
    }

    private void validateValue() {
        try {
            error = definition.isValid(textBox.getValue())
                    ? null : Component.literal(definition.validationMessage());
        } catch (RuntimeException exception) {
            error = Component.literal(definition.validationMessage());
        }
        if (done != null) done.active = error == null;
    }

    private void save() {
        validateValue();
        if (error == null && context.setValue(textBox.getValue())) closeToParent();
    }

    private void closeToParent() { openScreen(context.screen()); }

    @Override public void onClose() { closeToParent(); }

    //? if >=26 {
    /*@Override
    protected void extractConfigContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, title, width / 2, 10, 0xFFFFFFFF);
        if (error != null) drawCenteredText(graphics, error, width / 2, height - 42, 0xFFFF5555);
    }
     *///?} else {
    @Override protected void renderConfigContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, title, width / 2, 10, 0xFFFFFFFF);
        if (error != null) drawCenteredText(graphics, error, width / 2, height - 42, 0xFFFF5555);
    }
    //?}
}
