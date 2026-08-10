package net.syrupstudios.syruplibrary;

import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-neutral constants and bootstrap for Syrup Library. */
public final class SyrupLibrary {
    public static final String MOD_ID = "syrup_library";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized;

    private SyrupLibrary() {}

    /** Initializes shared services once, regardless of the active loader. */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        SyrupConfigManager.getInstance();
        initialized = true;
        LOGGER.info("{} initialized", MOD_ID);
    }
}
