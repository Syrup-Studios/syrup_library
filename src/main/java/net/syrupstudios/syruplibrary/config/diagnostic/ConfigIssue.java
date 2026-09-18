package net.syrupstudios.syruplibrary.config.diagnostic;

/**
 * A structured diagnostic produced while loading configuration.
 *
 * @param path config key, config ID, or file path associated with the issue
 * @param severity severity of the issue
 * @param message operator-facing explanation
 * @param originalValue rejected or adjusted value, when applicable
 * @param effectiveValue schema default or retained configured/startup value, when applicable
 */
public record ConfigIssue(
        String path,
        ConfigIssueSeverity severity,
        String message,
        Object originalValue,
        Object effectiveValue
) {
}
