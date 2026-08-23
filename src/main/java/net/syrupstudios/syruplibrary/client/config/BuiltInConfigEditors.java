package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigEditorHint;
import net.syrupstudios.syruplibrary.config.value.BooleanConfigValue;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;
import net.syrupstudios.syruplibrary.config.value.EnumConfigValue;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Factories for Syrup Library's standard value editors. */
final class BuiltInConfigEditors {
    private BuiltInConfigEditors() {
    }

    static ConfigEditorHandle create(ConfigEditorContext context) {
        ConfigEditorHint.Kind kind = context.node().presentation().editor().kind();
        if (kind == ConfigEditorHint.Kind.SLIDER) return SliderConfigEditor.create(context);
        if (kind == ConfigEditorHint.Kind.COLOR) return DialogConfigEditors.color(context);
        if (kind == ConfigEditorHint.Kind.MULTILINE) return DialogConfigEditors.multiline(context);
        if (kind == ConfigEditorHint.Kind.PATH) return DialogConfigEditors.path(context);

        ConfigValue<?> value = context.valueDefinition();
        if (value instanceof BooleanConfigValue) return booleanEditor(context);
        if (value instanceof EnumConfigValue<?> enumValue) return enumEditor(context, enumValue);
        if (value instanceof net.syrupstudios.syruplibrary.config.value.StringListConfigValue) {
            return DialogConfigEditors.stringList(context);
        }
        return textEditor(context);
    }

    private static ConfigEditorHandle booleanEditor(ConfigEditorContext context) {
        Button[] holder = new Button[1];
        holder[0] = Button.builder(Component.empty(), button -> {
            context.setValue(!((Boolean) context.value()));
            button.setMessage(Component.literal(String.valueOf(context.value())));
        }).bounds(0, 0, 80, 18).build();
        Runnable refresh = () -> holder[0].setMessage(Component.literal(String.valueOf(context.value())));
        refresh.run();
        return new SimpleConfigEditorHandle(holder[0], refresh);
    }

    private static ConfigEditorHandle enumEditor(ConfigEditorContext context, EnumConfigValue<?> enumValue) {
        Map<String, Enum<?>> constants = new LinkedHashMap<>();
        for (Object candidate : enumValue.enumType().getEnumConstants()) {
            Enum<?> constant = (Enum<?>) candidate;
            constants.put(constant.name().toLowerCase(Locale.ROOT), constant);
        }
        List<String> declaredOrder = context.node().presentation().enumPresentation().order();
        List<String> order = declaredOrder.isEmpty() ? List.copyOf(constants.keySet()) : declaredOrder;
        List<Enum<?>> choices = new ArrayList<>();
        for (String name : order) {
            if (!context.node().presentation().enumPresentation().disabled().contains(name))
                choices.add(constants.get(name));
        }
        if (choices.isEmpty()) choices.add((Enum<?>) context.value());
        Button[] holder = new Button[1];
        holder[0] = Button.builder(Component.empty(), ignored -> {
            Enum<?> current = (Enum<?>) context.value();
            int index = choices.indexOf(current);
            Enum<?> next = choices.get(index < 0 || index + 1 >= choices.size() ? 0 : index + 1);
            context.setValue(next);
            holder[0].setMessage(ConfigText.enumChoice(context.node(), (Enum<?>) context.value()));
        }).bounds(0, 0, 80, 18).build();
        Runnable refresh = () -> holder[0].setMessage(
                ConfigText.enumChoice(context.node(), (Enum<?>) context.value()));
        refresh.run();
        return new SimpleConfigEditorHandle(holder[0], refresh);
    }

    private static ConfigEditorHandle textEditor(ConfigEditorContext context) {
        EditBox editBox = new EditBox(context.font(), 0, 0, 80, 18, Component.empty());
        editBox.setMaxLength(Integer.MAX_VALUE);
        boolean[] syncing = {false};
        editBox.setResponder(text -> {
            if (!syncing[0]) context.setValue(ConfigText.parseText(context.valueDefinition(), text));
        });
        Runnable refresh = () -> {
            syncing[0] = true;
            try {
                editBox.setValue(ConfigText.toText(context.value()));
            } finally {
                syncing[0] = false;
            }
        };
        refresh.run();
        return new SimpleConfigEditorHandle(editBox, refresh);
    }
}
