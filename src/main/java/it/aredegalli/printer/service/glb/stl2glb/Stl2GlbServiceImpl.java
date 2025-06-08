package it.aredegalli.printer.service.glb.stl2glb;

import it.aredegalli.printer.client.Stl2GlbClient;
import it.aredegalli.printer.dto.glb.stl2glb.Stl2GlbRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Stl2GlbServiceImpl implements Stl2GlbService {

    private final Stl2GlbClient stl2GlbClient;

    @Override
    public String convertStlToGlb(String stlHash) {
        log.debug("Converting STL to GLB for hash: {}", stlHash);
        try {
            return stl2GlbClient.convertStlToGlb(Stl2GlbRequestDto.builder()
                    .stl_hash(stlHash)
                    .build()).getGlb_hash();
        } catch (Exception e) {
            log.error("Failed to convert STL to GLB for hash: {}", stlHash, e);
            throw new RuntimeException("Conversion failed", e);
        }
    }

}
