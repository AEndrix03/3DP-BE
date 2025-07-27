package it.aredegalli.printer.enums.slicing;

import java.util.Arrays;

/**
 * Enum for slicing queue status with utility methods
 */
public enum SlicingStatus {
    QUEUED("QUE", "Queued"),
    PROCESSING("PRO", "Processing"),
    COMPLETED("COM", "Completed"),
    FAILED("FAI", "Failed"),
    CANCELLED("CAN", "Cancelled");

    private final String code;
    private final String description;

    SlicingStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Convert from status code to enum
     */
    public static SlicingStatus fromCode(String code) {
        if (code == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown slicing status code: " + code));
    }

    /**
     * Check if status represents a terminal state (completed or failed)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * Check if status represents an active state (can be cancelled)
     */
    public boolean isCancellable() {
        return this == QUEUED || this == PROCESSING;
    }

    /**
     * Check if status represents a processing state
     */
    public boolean isProcessing() {
        return this == PROCESSING;
    }

    /**
     * Check if status represents a successful completion
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    @Override
    public String toString() {
        return description;
    }
}