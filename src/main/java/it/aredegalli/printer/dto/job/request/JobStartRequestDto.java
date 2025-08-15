package it.aredegalli.printer.dto.job.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStartRequestDto {

    private UUID printerId;
    private UUID slicingId;

    private Integer startOffset;

}
