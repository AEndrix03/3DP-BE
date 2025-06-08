package it.aredegalli.printer.client;

import it.aredegalli.printer.dto.glb.stl2glb.Stl2GlbRequestDto;
import it.aredegalli.printer.dto.glb.stl2glb.Stl2GlbResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "stl2glb", url = "${stl2glb.url}")
public interface Stl2GlbClient {

    @PostMapping(value = "/api/convert")
    Stl2GlbResponseDto convertStlToGlb(@RequestBody Stl2GlbRequestDto stlHash);

}
