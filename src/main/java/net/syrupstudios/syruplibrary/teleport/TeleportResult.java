package net.syrupstudios.syruplibrary.teleport;

/** The outcome of a teleport request. */
public enum TeleportResult {
    SUCCESS,
    INVALID_DESTINATION,
    UNKNOWN_DIMENSION,
    WRONG_THREAD,
    TELEPORT_REJECTED
}
