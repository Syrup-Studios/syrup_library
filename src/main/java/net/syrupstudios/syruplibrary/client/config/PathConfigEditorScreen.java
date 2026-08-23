package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigEditorHint;
import net.syrupstudios.syruplibrary.config.ConfigPathMode;
//? if >=26 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
 *///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Safe in-game path browser rooted at the loader config directory. */
final class PathConfigEditorScreen extends AbstractConfigScreen {
    private static final int PAGE_SIZE = 4;

    private final ConfigEditorContext context;
    private final ConfigEditorHint hint;
    private final Path root;
    private Path directory;
    private List<Path> entries = List.of();
    private int page;
    private EditBox valueBox;
    private Button upButton;
    private Button useDirectoryButton;
    private Button previousPage;
    private Button nextPage;
    private Button doneButton;
    private final List<Button> entryButtons = new ArrayList<>();
    private Component error;

    PathConfigEditorScreen(ConfigEditorContext context) {
        super(Component.translatable("syrup_library.config.path_editor"));
        this.context = context;
        this.hint = context.node().presentation().editor();
        this.root = context.session().config().path().getParent().toAbsolutePath().normalize();
        this.directory = initialDirectory(String.valueOf(context.value()));
    }

    @Override protected void init() {
        int center = width / 2;
        valueBox = new EditBox(font, center - 170, 30, 340, 20, Component.empty());
        valueBox.setMaxLength(Integer.MAX_VALUE);
        valueBox.setValue(String.valueOf(context.value()));
        valueBox.setResponder(ignored -> validateValue());
        addRenderableWidget(valueBox);
        upButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.path_up"), button -> navigateUp())
                .bounds(center - 170, 56, 82, 20).build());
        useDirectoryButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.path_use_folder"), button -> useDirectory())
                .bounds(center - 82, 56, 164, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.path_root"), button -> navigate(root))
                .bounds(center + 88, 56, 82, 20).build());
        for (int index = 0; index < PAGE_SIZE; index++) {
            final int slot = index;
            Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> selectSlot(slot))
                    .bounds(center - 170, 82 + index * 24, 340, 20).build());
            entryButtons.add(button);
        }
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(center - 170, 180, 40, 20).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(center + 130, 180, 40, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.cancel"), button -> closeToParent())
                .bounds(center - 154, height - 28, 150, 20).build());
        doneButton = addRenderableWidget(Button.builder(Component.translatable("syrup_library.config.done"), button -> done())
                .bounds(center + 4, height - 28, 150, 20).build());
        refreshEntries();
        validateValue();
    }

    private Path initialDirectory(String stored) {
        try {
            Path candidate = root.resolve(stored).normalize();
            if (candidate.startsWith(root)) {
                if (Files.isDirectory(candidate)) return candidate;
                Path parent = candidate.getParent();
                if (parent != null && parent.startsWith(root) && Files.isDirectory(parent)) return parent;
            }
        } catch (RuntimeException ignored) {
        }
        return root;
    }

    private void refreshEntries() {
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            entries = stream.filter(path -> Files.isDirectory(path) || acceptsExtension(path))
                    .sorted(Comparator.comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
            error = null;
        } catch (IOException exception) {
            entries = List.of();
            error = Component.translatable("syrup_library.config.path_read_failed");
        }
        int maximumPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, maximumPage));
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = page * PAGE_SIZE + slot;
            Button button = entryButtons.get(slot);
            button.visible = index < entries.size();
            if (button.visible) {
                Path entry = entries.get(index);
                String prefix = Files.isDirectory(entry) ? "[+] " : "";
                button.setMessage(Component.literal(prefix + entry.getFileName()));
            }
        }
        previousPage.active = page > 0;
        nextPage.active = (page + 1) * PAGE_SIZE < entries.size();
        upButton.active = !directory.equals(root);
        useDirectoryButton.active = hint.pathMode() != ConfigPathMode.FILE;
    }

    private boolean acceptsExtension(Path path) {
        if (hint.pathMode() == ConfigPathMode.DIRECTORY) return false;
        if (hint.extensions().contains("*")) return true;
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && hint.extensions().contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private void selectSlot(int slot) {
        int index = page * PAGE_SIZE + slot;
        if (index >= entries.size()) return;
        Path selected = entries.get(index);
        if (Files.isDirectory(selected)) navigate(selected);
        else {
            valueBox.setValue(relative(selected));
            validateValue();
        }
    }

    private void navigate(Path next) {
        Path normalized = next.toAbsolutePath().normalize();
        if (!normalized.startsWith(root) || !Files.isDirectory(normalized)) return;
        directory = normalized;
        page = 0;
        refreshEntries();
    }

    private void navigateUp() {
        Path parent = directory.getParent();
        if (parent != null && parent.startsWith(root)) navigate(parent);
    }

    private void useDirectory() {
        valueBox.setValue(relative(directory));
        validateValue();
    }

    private void changePage(int change) {
        page += change;
        refreshEntries();
    }

    private String relative(Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private void validateValue() {
        error = null;
        try {
            String text = valueBox.getValue();
            Path relative = Path.of(text);
            if (relative.isAbsolute()) throw new IllegalArgumentException();
            Path resolved = root.resolve(relative).normalize();
            if (!resolved.startsWith(root)) throw new IllegalArgumentException();
            if (hint.pathMode() == ConfigPathMode.FILE && !Files.isRegularFile(resolved)) {
                error = Component.translatable("syrup_library.config.path_file_required");
            } else if (hint.pathMode() == ConfigPathMode.DIRECTORY && !Files.isDirectory(resolved)) {
                error = Component.translatable("syrup_library.config.path_folder_required");
            } else if (hint.pathMode() == ConfigPathMode.FILE_OR_DIRECTORY && !Files.exists(resolved)) {
                error = Component.translatable("syrup_library.config.path_missing");
            } else if (Files.isRegularFile(resolved) && !acceptsExtension(resolved)) {
                error = Component.translatable("syrup_library.config.path_extension_invalid");
            }
        } catch (RuntimeException exception) {
            error = Component.translatable("syrup_library.config.path_outside_root");
        }
        if (doneButton != null) doneButton.active = error == null;
    }

    private void done() {
        validateValue();
        if (error == null && context.setValue(valueBox.getValue().replace('\\', '/'))) closeToParent();
    }

    private void closeToParent() { openScreen(context.screen()); }
    @Override public void onClose() { closeToParent(); }

    //? if >=26 {
    /*@Override
    protected void extractConfigContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, title, width / 2, 10, 0xFFFFFFFF);
        drawCenteredText(graphics, Component.literal(relative(directory)), width / 2, 166, 0xFFA0A0A0);
        if (error != null) drawCenteredText(graphics, error, width / 2, 204, 0xFFFF5555);
    }
     *///?} else {
    @Override protected void renderConfigContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawCenteredText(graphics, title, width / 2, 10, 0xFFFFFFFF);
        drawCenteredText(graphics, Component.literal(relative(directory)), width / 2, 166, 0xFFA0A0A0);
        if (error != null) drawCenteredText(graphics, error, width / 2, 204, 0xFFFF5555);
    }
    //?}
}
