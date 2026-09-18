package net.syrupstudios.syruplibrary.client.config;

import de.marhali.json5.Json5;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.syrupstudios.syruplibrary.config.value.ConfigType;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Registry of client editors keyed by the core config type descriptor. */
public final class ConfigEditorRegistry {
    public interface Adapter<T> {
        void create(SyrupConfigScreen screen, ConfigValue<T> value, Object draft,
                    Consumer<Object> setDraft, Runnable changed, Runnable rebuild);
        default String metadata(ConfigValue<T> value) { return ""; }
        default Object draft(T value) { return value; }
        default T normalize(ConfigValue<T> value, Object draft) { return value.cast(draft); }
    }

    private static final Json5 JSON5 = new Json5();
    private static final Map<ConfigType<?>, Adapter<?>> ADAPTERS = new ConcurrentHashMap<>();
    private static final Adapter<Object> READ_ONLY = (screen, value, draft, set, changed, rebuild) ->
            screen.label("This setting has no editor.", 100);

    static {
        Adapter<Object> number = new NumberAdapter();
        ADAPTERS.put(ConfigType.BOOLEAN, new BooleanAdapter());
        ADAPTERS.put(ConfigType.INTEGER, number);
        ADAPTERS.put(ConfigType.LONG, number);
        ADAPTERS.put(ConfigType.DOUBLE, number);
        ADAPTERS.put(ConfigType.STRING, new TextAdapter());
        ADAPTERS.put(ConfigType.STRING_LIST, new ListAdapter());
    }

    private ConfigEditorRegistry() {}

    /** Registers a client editor for a custom config type. */
    public static <T> void register(ConfigType<T> type, Adapter<T> adapter) {
        ADAPTERS.put(type, adapter);
    }

    @SuppressWarnings("unchecked")
    static <T> Adapter<T> adapter(ConfigValue<T> value) {
        Adapter<T> adapter = (Adapter<T>) ADAPTERS.get(value.type());
        if (adapter != null) return adapter;
        if (!value.type().choices().isEmpty()) return (Adapter<T>) new ChoiceAdapter();
        return (Adapter<T>) READ_ONLY;
    }

    static <T> void create(SyrupConfigScreen screen, ConfigValue<T> value, Object draft,
                           Consumer<Object> setDraft, Runnable changed, Runnable rebuild) {
        adapter(value).create(screen, value, draft, setDraft, changed, rebuild);
    }

    static <T> Object draft(ConfigValue<T> value, Object current) {
        return adapter(value).draft(value.cast(current));
    }

    static <T> T normalize(ConfigValue<T> value, Object draft) {
        return value.validate(adapter(value).normalize(value, draft));
    }

    static <T> String metadata(ConfigValue<T> value) {
        String extra = adapter(value).metadata(value);
        for (var constraint : value.constraints()) {
            if (!extra.isEmpty()) extra += "\n";
            extra += constraint.description();
        }
        if (!extra.isEmpty()) extra = "\n" + extra;
        return value.path() + "\n" + String.join("\n", value.description()) + extra
                + (value.restartRequirement() == net.syrupstudios.syruplibrary.config.RestartRequirement.REQUIRED
                ? "\nRequires restart.\nActive value: " + value.get()
                + "\nConfigured value: " + value.configuredValue() : "");
    }

    private static final class TextAdapter implements Adapter<Object> {
        @Override public void create(SyrupConfigScreen screen, ConfigValue<Object> value, Object draft,
                                     Consumer<Object> set, Runnable changed, Runnable rebuild) {
            screen.textField((String) draft, value.path(), 100, Math.max(32, screen.clientHeight() - 184), text -> {
                set.accept(text); changed.run();
            });
        }
    }

    private static final class ListAdapter implements Adapter<Object> {
        @Override public Object draft(Object value) { return new ArrayList<>((List<?>) value); }
        @Override public void create(SyrupConfigScreen screen, ConfigValue<Object> value, Object draft,
                                     Consumer<Object> set, Runnable changed, Runnable rebuild) {
            screen.listEditor(value);
        }
    }

    private static final class BooleanAdapter implements Adapter<Object> {
        @Override public void create(SyrupConfigScreen screen, ConfigValue<Object> value, Object draft,
                                     Consumer<Object> set, Runnable changed, Runnable rebuild) {
            screen.button(value.path() + ": " + draft, screen.left(), 100, screen.contentWidth(), () -> {
                set.accept(!((Boolean) draft)); changed.run(); rebuild.run();
            });
        }
    }

    private static final class ChoiceAdapter implements Adapter<Object> {
        @Override public void create(SyrupConfigScreen screen, ConfigValue<Object> value, Object draft,
                                     Consumer<Object> set, Runnable changed, Runnable rebuild) {
            screen.button(value.path() + ": " + draft, screen.left(), 100, screen.contentWidth(), () -> {
                List<Object> choices = value.type().choices();
                set.accept(choices.get((choices.indexOf(draft) + 1) % choices.size()));
                changed.run(); rebuild.run();
            });
        }
        @Override public String metadata(ConfigValue<Object> value) { return "Choices: " + value.type().choices(); }
    }

    private static final class NumberAdapter implements Adapter<Object> {
        @Override public Object draft(Object value) { return String.valueOf(value); }
        @Override public Object normalize(ConfigValue<Object> value, Object draft) {
            return value.type().decode(element(String.valueOf(draft).trim()));
        }
        @Override public void create(SyrupConfigScreen screen, ConfigValue<Object> value, Object draft,
                                     Consumer<Object> set, Runnable changed, Runnable rebuild) {
            EditBox field = new EditBox(screen.clientFont(), screen.left(), 100, screen.contentWidth(), 20,
                    Component.literal(value.path()));
            field.setMaxLength(Integer.MAX_VALUE);
            field.setValue((String) draft);
            field.setResponder(text -> { set.accept(text); changed.run(); });
            screen.widget(field);
        }
    }

    private static de.marhali.json5.Json5Element element(String text) {
        var array = JSON5.parse("[" + text + "]").getAsJson5Array();
        if (array.size() != 1) throw new IllegalArgumentException("Expected one value");
        return array.get(0);
    }
}
