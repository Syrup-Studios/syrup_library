package net.syrupstudios.syruplibrary.config.diagnostic;

import java.util.List;

/** Result of saving the current configured configuration. */
public record ConfigSaveResult(boolean successful, List<ConfigIssue> issues, Throwable cause) {
    public ConfigSaveResult { issues = List.copyOf(issues); }
    public boolean hasErrors() { return issues.stream().anyMatch(i -> i.severity() == ConfigIssueSeverity.ERROR); }
}
