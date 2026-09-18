package net.syrupstudios.syruplibrary.config.value;

import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import de.marhali.json5.Json5Primitive;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Immutable descriptor for JSON5 encoding and Java value normalization. */
public final class ConfigType<T> {
    public static final ConfigType<Boolean> BOOLEAN = new ConfigType<>(Boolean.class,
            primitive(Json5Primitive::isBoolean, Json5Primitive::getAsBoolean, "boolean"),
            Json5Primitive::fromBoolean, Boolean.class::cast);
    public static final ConfigType<Integer> INTEGER = new ConfigType<>(Integer.class,
            primitive(Json5Primitive::isNumber, p -> p.getAsBigDecimal().intValueExact(), "integer"),
            Json5Primitive::fromNumber, Integer.class::cast);
    public static final ConfigType<Long> LONG = new ConfigType<>(Long.class,
            primitive(Json5Primitive::isNumber, p -> p.getAsBigDecimal().longValueExact(), "long"),
            Json5Primitive::fromNumber, Long.class::cast);
    public static final ConfigType<Double> DOUBLE = new ConfigType<>(Double.class,
            primitive(Json5Primitive::isNumber, p -> finite(p.getAsBigDecimal().doubleValue()), "finite double"),
            Json5Primitive::fromNumber, ConfigType::finiteDouble);
    public static final ConfigType<String> STRING = new ConfigType<>(String.class,
            primitive(Json5Primitive::isString, Json5Primitive::getAsString, "string"),
            Json5Primitive::fromString, String.class::cast);
    public static final ConfigType<List<String>> STRING_LIST = new ConfigType<>(List.class,
            element -> {
                if (!element.isJson5Array()) throw new IllegalArgumentException("Expected array of strings");
                List<String> result = new ArrayList<>();
                for (Json5Element item : element.getAsJson5Array()) {
                    if (!item.isJson5Primitive() || !item.getAsJson5Primitive().isString())
                        throw new IllegalArgumentException("Expected array containing only strings");
                    result.add(item.getAsJson5Primitive().getAsString());
                }
                return result;
            },
            value -> {
                Json5Array array = new Json5Array();
                for (String item : value) array.add(item);
                return array;
            },
            value -> {
                if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Expected list of strings");
                return list.stream().map(item -> {
                    if (!(item instanceof String text)) throw new IllegalArgumentException("Expected list containing only strings");
                    return text;
                }).toList();
            }, List.of(), STRING);

    private final Class<?> javaType;
    private final Function<Json5Element, T> decoder;
    private final Function<T, Json5Element> encoder;
    private final Function<Object, T> normalizer;
    private final List<T> choices;
    private final ConfigType<?> elementType;

    /**
     * Defines a custom representation. The normalizer must return an independent copy for
     * mutable values. It is used on storage and reads to keep snapshots isolated.
     */
    public ConfigType(Class<?> javaType, Function<Json5Element, T> decoder,
                      Function<T, Json5Element> encoder, Function<Object, T> normalizer) {
        this(javaType, decoder, encoder, normalizer, List.of(), null);
    }

    private ConfigType(Class<?> javaType, Function<Json5Element, T> decoder,
                       Function<T, Json5Element> encoder, Function<Object, T> normalizer,
                       List<T> choices, ConfigType<?> elementType) {
        this.javaType = Objects.requireNonNull(javaType, "javaType");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
        this.choices = List.copyOf(choices);
        this.elementType = elementType;
    }

    public static <E extends Enum<E>> ConfigType<E> enumType(Class<E> type) {
        Objects.requireNonNull(type, "type");
        E[] constants = Objects.requireNonNull(type.getEnumConstants(), "enum constants");
        Set<String> names = new HashSet<>();
        for (E value : constants) {
            if (!names.add(value.name().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Enum names must be unique ignoring case");
        }
        List<E> values = List.of(constants);
        return new ConfigType<>(type,
                element -> {
                    if (!element.isJson5Primitive() || !element.getAsJson5Primitive().isString())
                        throw new IllegalArgumentException("Expected enum name");
                    String name = element.getAsJson5Primitive().getAsString().toLowerCase(Locale.ROOT);
                    return values.stream().filter(v -> v.name().toLowerCase(Locale.ROOT).equals(name))
                            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown enum name"));
                },
                value -> Json5Primitive.fromString(value.name().toLowerCase(Locale.ROOT)),
                type::cast, values, null);
    }

    public Class<?> javaType() { return javaType; }

    public T decode(Json5Element element) {
        return normalize(Objects.requireNonNull(decoder.apply(Objects.requireNonNull(element, "element")), "decoder result"));
    }

    public Json5Element encode(T value) {
        T normalized = normalize(value);
        return Objects.requireNonNull(encoder.apply(normalized), "encoder result").deepCopy();
    }

    public T normalize(Object value) {
        if (!javaType.isInstance(value)) throw new IllegalArgumentException("Expected " + javaType.getSimpleName());
        T normalized = Objects.requireNonNull(normalizer.apply(value), "normalizer result");
        if (!javaType.isInstance(normalized)) throw new IllegalArgumentException("Normalizer returned the wrong type");
        return normalized;
    }

    public List<T> choices() { return choices; }
    public ConfigType<?> elementType() { return elementType; }

    private static <T> Function<Json5Element, T> primitive(
            Function<Json5Primitive, Boolean> predicate,
            Function<Json5Primitive, T> reader,
            String name) {
        return element -> {
            if (!element.isJson5Primitive() || !predicate.apply(element.getAsJson5Primitive()))
                throw new IllegalArgumentException("Expected " + name);
            return reader.apply(element.getAsJson5Primitive());
        };
    }

    private static double finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Expected finite double");
        return value;
    }

    private static Double finiteDouble(Object value) {
        if (!(value instanceof Double number)) throw new IllegalArgumentException("Expected finite double");
        return finite(number);
    }
}
