package it.aredegalli.printer.model.slicing;

public enum ContainerStatus {
    HEALTHY("healthy"),
    UNHEALTHY("unhealthy"),
    UNKNOWN("unknown"),
    MAINTENANCE("maintenance"),
    STOPPED("stopped");

    private final String displayName;

    ContainerStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}