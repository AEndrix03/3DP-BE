package it.aredegalli.printer.dto.printer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrinterFilterDto {

    private String name;
    private String driverId;
    private String status;

    /**
     * Check if any filter is active
     */
    public boolean hasAnyFilter() {
        return (name != null && !name.trim().isEmpty()) ||
                (driverId != null && !driverId.trim().isEmpty()) ||
                (status != null && !status.trim().isEmpty());
    }

    /**
     * Check if name filter is active
     */
    public boolean hasNameFilter() {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * Check if driverId filter is active
     */
    public boolean hasDriverIdFilter() {
        return driverId != null && !driverId.trim().isEmpty();
    }

    /**
     * Check if status filter is active
     */
    public boolean hasStatusFilter() {
        return status != null && !status.trim().isEmpty();
    }

    /**
     * Get trimmed name for case-insensitive search
     */
    public String getNameTrimmed() {
        return hasNameFilter() ? name.trim().toLowerCase() : null;
    }

    /**
     * Get trimmed driverId for search
     */
    public String getDriverIdTrimmed() {
        return hasDriverIdFilter() ? driverId.trim() : null;
    }

    /**
     * Get trimmed and uppercased status for search
     */
    public String getStatusTrimmed() {
        return hasStatusFilter() ? status.trim().toUpperCase() : null;
    }
}