package it.aredegalli.printer.service.material;

import it.aredegalli.printer.mapper.material.MaterialMapper;
import it.aredegalli.printer.repository.material.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private MaterialRepository materialRepository;
    private MaterialMapper materialMapper;


}
