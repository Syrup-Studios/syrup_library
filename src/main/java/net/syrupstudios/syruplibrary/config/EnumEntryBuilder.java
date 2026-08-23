package net.syrupstudios.syruplibrary.config;

import net.syrupstudios.syruplibrary.config.value.EnumConfigValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/** Builder for an enum config value with optional choice presentation. */
public final class EnumEntryBuilder<E extends Enum<E>>
        extends ConfigEntryBuilder<E, EnumConfigValue<E>, EnumEntryBuilder<E>> {
    private final Class<E> enumType;
    private final E defaultValue;
    private List<E> optionOrder = List.of();
    private final Map<E, String> optionLabels = new LinkedHashMap<>();
    private Predicate<E> enabledOptions = ignored -> true;

    EnumEntryBuilder(ConfigSpec spec, ConfigSchemaNode parent, String key, Class<E> enumType, E defaultValue) {
        super(spec, parent, key);
        this.enumType = Objects.requireNonNull(enumType, "enumType");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
    }

    public EnumEntryBuilder<E> optionOrder(List<E> values) {
        this.optionOrder = List.copyOf(values);
        return this;
    }

    @SafeVarargs
    public final EnumEntryBuilder<E> optionOrder(E... values) {
        return optionOrder(List.of(values));
    }

    public EnumEntryBuilder<E> optionLabel(E value, String translationKey) {
        optionLabels.put(Objects.requireNonNull(value, "value"),
                ConfigPresentation.translationKey(translationKey));
        return this;
    }

    public EnumEntryBuilder<E> enabledOptions(Predicate<E> predicate) {
        this.enabledOptions = Objects.requireNonNull(predicate, "predicate");
        return this;
    }

    @Override protected EnumEntryBuilder<E> self() { return this; }

    @Override protected void preparePresentation() {
        List<E> constants = List.of(enumType.getEnumConstants());
        List<E> order = optionOrder.isEmpty() ? constants : optionOrder;
        if (new LinkedHashSet<>(order).size() != order.size() || !new LinkedHashSet<>(order).containsAll(constants)
                || order.size() != constants.size()) {
            throw new IllegalArgumentException("Enum option order must contain every constant exactly once for " + key);
        }
        List<String> serializedOrder = new ArrayList<>();
        Map<String, String> labels = new LinkedHashMap<>();
        Set<String> disabled = new LinkedHashSet<>();
        for (E value : order) serializedOrder.add(serialize(value));
        optionLabels.forEach((value, label) -> {
            if (!enumType.isInstance(value)) {
                throw new IllegalArgumentException("Enum option label has the wrong type for " + key);
            }
            labels.put(serialize(value), label);
        });
        for (E value : constants) if (!enabledOptions.test(value)) disabled.add(serialize(value));
        presentation.enumPresentation(new EnumPresentation(serializedOrder, labels, disabled));
    }

    @Override protected EnumConfigValue<E> createValue(String path) {
        return new EnumConfigValue<>(spec, key, path, enumType, defaultValue, description, restartRequirement);
    }

    private static String serialize(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }
}
