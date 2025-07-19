package it.aredegalli.printer.enums.slicing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum SlicingStatus {
    QUEUED("QUEUED", "In coda"),
    PROCESSING("PROCESSING", "In elaborazione"),
    COMPLETED("COMPLETED", "Completato"),
    FAILED("FAILED", "Fallito"),
    CANCELLED("CANCELLED", "Annullato");

    private final String code;
    private final String description;
}