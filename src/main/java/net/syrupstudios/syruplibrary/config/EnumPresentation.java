package net.syrupstudios.syruplibrary.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable presentation metadata for enum choices, keyed by their stable serialized names. */
public final class EnumPresentation {
    public static final EnumPresentation DEFAULT = new EnumPresentation(List.of(), Map.of(), Set.of());

    private final List<String> order;
    private final Map<String, String> translationKeys;
    private final Set<String> disabled;

    EnumPresentation(List<String> order, Map<String, String> translationKeys, Set<String> disabled) {
        this.order = List.copyOf(order);
        this.translationKeys = Map.copyOf(translationKeys);
        this.disabled = Set.copyOf(disabled);
    }

    public List<String> order() { return order; }

    public Map<String, String> translationKeys() { return translationKeys; }

    public Set<String> disabled() { return disabled; }
}
