package it.aredegalli.printer.model.slicing;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Enhanced ContainerStatus enum with proper database mapping
 */
public enum ContainerStatus {
    HEALTHY("healthy"),
    UNHEALTHY("unhealthy"),
    UNKNOWN("unknown"),
    MAINTENANCE("maintenance"),
    STOPPED("stopped"),
    STARTING("starting"),
    ERROR("error");

    private final String databaseValue;

    ContainerStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public String getDisplayName() {
        return databaseValue;
    }

    @Override
    public String toString() {
        return databaseValue;
    }

    /**
     * Get enum from database value with fallback
     */
    public static ContainerStatus fromDatabaseValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        for (ContainerStatus status : values()) {
            if (status.databaseValue.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        // Fallback for unknown values
        return UNKNOWN;
    }

    /**
     * Custom JPA converter for proper database mapping
     */
    @Converter(autoApply = true)
    public static class ContainerStatusConverter implements AttributeConverter<ContainerStatus, String> {

        @Override
        public String convertToDatabaseColumn(ContainerStatus attribute) {
            return attribute != null ? attribute.getDatabaseValue() : UNKNOWN.getDatabaseValue();
        }

        @Override
        public ContainerStatus convertToEntityAttribute(String dbData) {
            return ContainerStatus.fromDatabaseValue(dbData);
        }
    }
}