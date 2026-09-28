package net.syrupstudios.syruplibrary;

import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.RestartRequirement;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import net.syrupstudios.syruplibrary.config.value.ConfigConstraint;
import net.syrupstudios.syruplibrary.config.value.ConfigType;

import java.util.List;

/** Example settings for checking the config screen and its built-in editors. */
final class ExampleConfig {
    private enum DisplayMode { COMPACT, DETAILED, HIDDEN }

    private ExampleConfig() {}

    static void register() {
        ConfigSpec spec = ConfigSpec.builder("syrup_library_examples")
                .owner(SyrupLibrary.MOD_ID)
                .header("Syrup Library config editor examples")
                .build();
        spec.bool("enabled", true, "Example toggle setting.");
        spec.value("sample_count", ConfigType.INTEGER, 8,
                "Restart-required integer, limited to 1 through 32.", RestartRequirement.REQUIRED,
                List.of(ConfigConstraint.range(1, 32)));
        spec.longValue("sample_id", 1234567890123L, "Example long number.");
        spec.doubleValue("sample_scale", 1.25, "Example decimal number.");
        spec.string("sample_name", "Syrup", "Example text setting.");
        spec.stringList("sample_labels", List.of("first", "second", "third"),
                "Example list of text values.");
        spec.enumValue("display_mode", DisplayMode.DETAILED, "Example choice with three enum values.");
        SyrupConfigManager.getInstance().register(spec);
    }
}
