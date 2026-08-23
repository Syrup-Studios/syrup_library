package net.syrupstudios.syruplibrary.client.config;

import net.syrupstudios.syruplibrary.config.ConfigEditorHint;
import net.syrupstudios.syruplibrary.config.value.DoubleConfigValue;
import net.syrupstudios.syruplibrary.config.value.IntConfigValue;
import net.syrupstudios.syruplibrary.config.value.LongConfigValue;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Built-in bounded-number slider editor. */
final class SliderConfigEditor {
    private SliderConfigEditor() {
    }

    static ConfigEditorHandle create(ConfigEditorContext context) {
        ValueSlider slider = new ValueSlider(context);
        return new SimpleConfigEditorHandle(slider, () -> slider.sync(context.value()));
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final ConfigEditorContext context;
        private boolean syncing;

        ValueSlider(ConfigEditorContext context) {
            super(0, 0, 80, 18, Component.empty(), normalized(context));
            this.context = context;
            updateMessage();
        }

        void sync(Object current) {
            syncing = true;
            try {
                value = normalized(context);
                updateMessage();
            } finally {
                syncing = false;
            }
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.valueOf(context == null ? "" : context.value())));
        }

        @Override
        protected void applyValue() {
            if (syncing) return;
            Object candidate = denormalized(context, value);
            context.setValue(candidate);
            updateMessage();
        }
    }

    private static double normalized(ConfigEditorContext context) {
        Object current = context.value();
        if (context.valueDefinition() instanceof IntConfigValue value) {
            if (value.minimum() == value.maximum()) return 0;
            return clamp(((Integer) current - (double) value.minimum()) / (value.maximum() - (double) value.minimum()));
        }
        if (context.valueDefinition() instanceof LongConfigValue value) {
            BigDecimal offset = BigDecimal.valueOf((Long) current).subtract(BigDecimal.valueOf(value.minimum()));
            BigDecimal range = BigDecimal.valueOf(value.maximum()).subtract(BigDecimal.valueOf(value.minimum()));
            return range.signum() == 0 ? 0 : clamp(offset.divide(range, 16, RoundingMode.HALF_UP).doubleValue());
        }
        DoubleConfigValue value = (DoubleConfigValue) context.valueDefinition();
        BigDecimal minimum = BigDecimal.valueOf(value.minimum());
        BigDecimal offset = BigDecimal.valueOf((Double) current).subtract(minimum);
        BigDecimal range = BigDecimal.valueOf(value.maximum()).subtract(minimum);
        return range.signum() == 0 ? 0 : clamp(offset.divide(range, 16, RoundingMode.HALF_UP).doubleValue());
    }

    private static Object denormalized(ConfigEditorContext context, double position) {
        ConfigEditorHint hint = context.node().presentation().editor();
        if (context.valueDefinition() instanceof IntConfigValue value) {
            double raw = value.minimum() + position * (value.maximum() - (double) value.minimum());
            int step = hint.step() == null ? 1 : hint.step().intValue();
            long stepped = Math.round((raw - value.minimum()) / step) * (long) step + value.minimum();
            return (int) Math.max(value.minimum(), Math.min(value.maximum(), stepped));
        }
        if (context.valueDefinition() instanceof LongConfigValue value) {
            BigDecimal minimum = BigDecimal.valueOf(value.minimum());
            BigDecimal range = BigDecimal.valueOf(value.maximum()).subtract(minimum);
            BigDecimal raw = minimum.add(range.multiply(BigDecimal.valueOf(position)));
            long step = hint.step() == null ? 1 : hint.step().longValue();
            BigDecimal units = raw.subtract(minimum).divide(BigDecimal.valueOf(step), 0, RoundingMode.HALF_UP);
            BigDecimal stepped = minimum.add(units.multiply(BigDecimal.valueOf(step)));
            return stepped.max(minimum).min(BigDecimal.valueOf(value.maximum())).longValueExact();
        }
        DoubleConfigValue value = (DoubleConfigValue) context.valueDefinition();
        BigDecimal minimum = BigDecimal.valueOf(value.minimum());
        BigDecimal maximum = BigDecimal.valueOf(value.maximum());
        BigDecimal range = maximum.subtract(minimum);
        BigDecimal raw = minimum.add(range.multiply(BigDecimal.valueOf(position)));
        if (hint.step() != null) {
            BigDecimal step = BigDecimal.valueOf(hint.step().doubleValue());
            BigDecimal units = raw.subtract(minimum).divide(step, 0, RoundingMode.HALF_UP);
            raw = minimum.add(units.multiply(step));
        }
        return raw.max(minimum).min(maximum).doubleValue();
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
