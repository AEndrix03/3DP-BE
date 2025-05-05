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
    CANCELLED("CNC");

    private final String code;

}
