package it.aredegalli.printer.dto.glb.stl2glb;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Stl2GlbRequestDto {

    @NotNull
    @NotBlank
    private final String stl_hash;

}
