package net.syrupstudios.syruplibrary.config.value;

import java.util.Objects;
import java.util.function.Predicate;

/** Reusable validation rule for a configuration value. */
@FunctionalInterface
public interface ConfigConstraint<T> {
    /** Returns null for a valid value, or a diagnostic message for a rejected value. */
    String validate(T value);

    default String description() { return "Value failed validation"; }

    static <T> ConfigConstraint<T> predicate(Predicate<? super T> predicate, String message) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(message, "message");
        return new ConfigConstraint<>() {
            @Override public String validate(T value) { return predicate.test(value) ? null : message; }
            @Override public String description() { return message; }
        };
    }

    static <T extends Comparable<? super T>> Range<T> range(T minimum, T maximum, String description) {
        return new Range<>(minimum, maximum, description);
    }

    static <T extends Comparable<? super T>> Range<T> range(T minimum, T maximum) {
        return range(minimum, maximum, "Range: " + minimum + " ~ " + maximum);
    }

    record Range<T extends Comparable<? super T>>(T minimum, T maximum, String description)
            implements ConfigConstraint<T> {
        public Range {
            Objects.requireNonNull(minimum, "minimum");
            Objects.requireNonNull(maximum, "maximum");
            Objects.requireNonNull(description, "description");
            if (minimum instanceof Double number && !Double.isFinite(number)
                    || maximum instanceof Double upper && !Double.isFinite(upper)) {
                throw new IllegalArgumentException("Range limits must be finite");
            }
            if (minimum.compareTo(maximum) > 0) throw new IllegalArgumentException("minimum exceeds maximum");
        }

        @Override public String validate(T value) {
            return value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0 ? description : null;
        }
    }
}
