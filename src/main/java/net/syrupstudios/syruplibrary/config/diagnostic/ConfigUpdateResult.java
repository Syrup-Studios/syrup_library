package net.syrupstudios.syruplibrary.config.diagnostic;

import java.util.List;

/** Result of a programmatic configuration update. */
public record ConfigUpdateResult(boolean successful, List<ConfigIssue> issues, Throwable cause) {
    public ConfigUpdateResult { issues = List.copyOf(issues); }
    public boolean hasErrors() { return issues.stream().anyMatch(i -> i.severity() == ConfigIssueSeverity.ERROR); }
}
