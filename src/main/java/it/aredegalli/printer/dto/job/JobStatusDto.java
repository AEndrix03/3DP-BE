package it.aredegalli.printer.dto.job;

import it.aredegalli.printer.dto.common.CodeDescriptionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JobStatusDto extends CodeDescriptionDto {
}
