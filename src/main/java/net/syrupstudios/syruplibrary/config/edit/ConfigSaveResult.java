package net.syrupstudios.syruplibrary.config.edit;

/**
 * Structured outcome of a config save attempt.
 *
 * @param success whether the save completed and the new snapshot was published
 * @param fileChanged whether an external file edit prevented the save
 * @param message operator-facing explanation
 */
public record ConfigSaveResult(
        boolean success,
        boolean fileChanged,
        String message
) {
    public static ConfigSaveResult ok() {
        return new ConfigSaveResult(true, false, "");
    }

    public static ConfigSaveResult failure(boolean fileChanged, String message) {
        return new ConfigSaveResult(false, fileChanged, message);
    }
}
