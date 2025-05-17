package it.aredegalli.printer.enums.job;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class JobStatusEnumConverter implements AttributeConverter<JobStatusEnum, String> {

    @Override
    public String convertToDatabaseColumn(JobStatusEnum status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public JobStatusEnum convertToEntityAttribute(String code) {
        if (code == null) return null;
        return Stream.of(JobStatusEnum.values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid JobStatus code: " + code));
    }
}
