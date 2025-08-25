package it.aredegalli.printer.enums.job;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum JobStatusEnum {

    CREATED("CRE"),
    QUEUED("QUE"),
    RUNNING("RUN"),
    PAUSED("PAU"),
    COMPLETED("CMP"),
    FAILED("FAI"),
    CANCELLED("CNC"),
    PRECHECK("PRE"),
    HOMING("HOM"),
    LOADING("LOA"),
    HEATING("HEA");

    private final String code;

    public static JobStatusEnum decode(String code) {
        for (JobStatusEnum status : JobStatusEnum.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown JobStatusEnum code: " + code);
    }

}
