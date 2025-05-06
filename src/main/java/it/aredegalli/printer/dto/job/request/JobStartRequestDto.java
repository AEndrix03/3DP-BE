package it.aredegalli.printer.dto.job.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStartRequestDto {

    private Long startOffsetLine;

}
