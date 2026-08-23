package net.syrupstudios.syruplibrary.client.config;

//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** In-game hexadecimal and RGB(A) color editor. */
final class ColorConfigEditorScreen extends AbstractConfigScreen {
    private final ConfigEditorContext context;
    private final boolean alpha;
    private EditBox hexBox;
    private EditBox redBox;
    private EditBox greenBox;
    private EditBox blueBox;
    private EditBox alphaBox;
    private Button preview;
    private Button doneButton;
    private Component error;
    private boolean syncing;

    ColorConfigEditorScreen(ConfigEditorContext context) {
        super(Component.translatable("syrup_library.config.color_editor"));
        this.context = context;
        this.alpha = context.node().presentation().editor().alpha();
    }

    @Override
    protected void init() {
        int center = width / 2;
        hexBox = componentBox(center - 150, 38, 300, text -> updateFromHex());
        redBox = componentBox(center - 150, 68, 68, text -> updateFromComponents());
        greenBox = componentBox(center - 74, 68, 68, text -> updateFromComponents());
        blueBox = componentBox(center + 2, 68, 68, text -> updateFromComponents());
        if (alpha) alphaBox = componentBox(center + 78, 68, 72, text -> updateFromComponents());
        preview = addRenderableWidget(Button.builder(Component.empty(), button -> {})
                .bounds(center - 60, 98, 120, 20).build());
        preview.active = false;
        addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.cancel"), button -> closeToParent())
                .bounds(center - 154, height - 28, 150, 20).build());
        doneButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.done"), button -> done())
                .bounds(center + 4, height - 28, 150, 20).build());
        syncing = true;
        hexBox.setValue(String.valueOf(context.value()));
        syncing = false;
        updateFromHex();
    }

    private EditBox componentBox(int x, int y, int width, java.util.function.Consumer<String> responder) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setMaxLength(3);
        box.setResponder(responder);
        addRenderableWidget(box);
        return box;
    }

    private void updateFromHex() {
        if (syncing) return;
        String text = hexBox.getValue().trim();
        if (text.startsWith("#")) text = text.substring(1);
        int expected = alpha ? 8 : 6;
        if (text.length() != expected || !text.matches("[0-9a-fA-F]+")) {
            setError("syrup_library.config.color_invalid");
            return;
        }
        try {
            int red = Integer.parseInt(text.substring(0, 2), 16);
            int green = Integer.parseInt(text.substring(2, 4), 16);
            int blue = Integer.parseInt(text.substring(4, 6), 16);
            int opacity = alpha ? Integer.parseInt(text.substring(6, 8), 16) : 255;
            syncing = true;
            redBox.setValue(String.valueOf(red));
            greenBox.setValue(String.valueOf(green));
            blueBox.setValue(String.valueOf(blue));
            if (alphaBox != null) alphaBox.setValue(String.valueOf(opacity));
            syncing = false;
            setValid(red, green, blue);
        } catch (NumberFormatException exception) {
            syncing = false;
            setError("syrup_library.config.color_invalid");
        }
    }

    private void updateFromComponents() {
        if (syncing) return;
        try {
            int red = component(redBox);
            int green = component(greenBox);
            int blue = component(blueBox);
            int opacity = alphaBox == null ? 255 : component(alphaBox);
            String value = alpha ? String.format("#%02X%02X%02X%02X", red, green, blue, opacity)
                    : String.format("#%02X%02X%02X", red, green, blue);
            syncing = true;
            hexBox.setValue(value);
            syncing = false;
            setValid(red, green, blue);
        } catch (IllegalArgumentException exception) {
            syncing = false;
            setError("syrup_library.config.color_component_invalid");
        }
    }

    private static int component(EditBox box) {
        int value = Integer.parseInt(box.getValue());
        if (value < 0 || value > 255) throw new IllegalArgumentException();
        return value;
    }

    private void setValid(int red, int green, int blue) {
        error = null;
        if (doneButton != null) doneButton.active = true;
        int color = red << 16 | green << 8 | blue;
        preview.setMessage(Component.literal("████").setStyle(Style.EMPTY.withColor(color)));
    }

    private void setError(String key) {
        error = Component.translatable(key);
        if (doneButton != null) doneButton.active = false;
        if (preview != null) preview.setMessage(Component.literal("—"));
    }

    private void done() {
        updateFromHex();
        if (error == null && context.setValue(hexBox.getValue())) closeToParent();
        else if (error == null) error = Component.literal(context.session().errorFor(context.valueDefinition()));
    }

    private void closeToParent() { openScreen(context.screen()); }

    @Override public void onClose() { closeToParent(); }

    //? if >=26 {
    /*@Override
    protected void extractConfigContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, title, width / 2, 10, 0xFFFFFFFF);
        if (error != null) drawCenteredText(graphics, error, width / 2, 124, 0xFFFF5555);
    }
     *///?} else {
    @Override
    protected void renderConfigContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, title, width / 2, 10, 0xFFFFFFFF);
        if (error != null) drawCenteredText(graphics, error, width / 2, 124, 0xFFFF5555);
    }
    //?}
}
