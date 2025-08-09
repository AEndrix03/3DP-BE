package it.aredegalli.printer.repository.material;

import it.aredegalli.printer.model.material.MaterialType;
import it.aredegalli.printer.repository.UUIDRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialTypeRepository extends UUIDRepository<MaterialType> {

    List<MaterialType> findByIsActiveTrue();

    Optional<MaterialType> findByNameAndIsActiveTrue(String name);

    @Query("SELECT mt FROM MaterialType mt WHERE mt.isActive = true ORDER BY mt.name")
    List<MaterialType> findAllActiveOrderByName();

    @Query("SELECT mt FROM MaterialType mt WHERE mt.isFlexible = :flexible AND mt.isActive = true")
    List<MaterialType> findByIsFlexibleAndActive(@Param("flexible") Boolean flexible);
}