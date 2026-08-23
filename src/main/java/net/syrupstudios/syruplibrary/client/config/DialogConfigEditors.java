package net.syrupstudios.syruplibrary.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Button editors that open a dedicated screen for complex values. */
final class DialogConfigEditors {
    private DialogConfigEditors() {
    }

    static ConfigEditorHandle stringList(ConfigEditorContext context) {
        Button[] holder = new Button[1];
        holder[0] = Button.builder(Component.empty(), button ->
                context.openScreen(new StringListEditorScreen(context)))
                .bounds(0, 0, 80, 18).build();
        Runnable refresh = () -> {
            int size = ((List<?>) context.value()).size();
            holder[0].setMessage(Component.translatable("syrup_library.config.list_items", size));
        };
        refresh.run();
        return new SimpleConfigEditorHandle(holder[0], refresh);
    }

    static ConfigEditorHandle color(ConfigEditorContext context) {
        return button(context, () -> new ColorConfigEditorScreen(context));
    }

    static ConfigEditorHandle multiline(ConfigEditorContext context) {
        return button(context, () -> new MultilineConfigEditorScreen(context));
    }

    static ConfigEditorHandle path(ConfigEditorContext context) {
        return button(context, () -> new PathConfigEditorScreen(context));
    }

    private static ConfigEditorHandle button(ConfigEditorContext context,
                                             java.util.function.Supplier<AbstractConfigScreen> screen) {
        Button[] holder = new Button[1];
        holder[0] = Button.builder(Component.empty(), button -> context.openScreen(screen.get()))
                .bounds(0, 0, 80, 18).build();
        Runnable refresh = () -> holder[0].setMessage(Component.literal(summary(String.valueOf(context.value()))));
        refresh.run();
        return new SimpleConfigEditorHandle(holder[0], refresh);
    }

    private static String summary(String value) {
        if (value.isEmpty()) return "…";
        return value.length() <= 24 ? value : value.substring(0, 21) + "...";
    }
}
