package it.aredegalli.printer.repository.material;

import it.aredegalli.printer.model.material.MaterialBrand;
import it.aredegalli.printer.repository.UUIDRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialBrandRepository extends UUIDRepository<MaterialBrand> {

    List<MaterialBrand> findByIsActiveTrue();

    Optional<MaterialBrand> findByNameAndIsActiveTrue(String name);

    @Query("SELECT mb FROM MaterialBrand mb WHERE mb.isActive = true ORDER BY mb.qualityRating DESC, mb.name")
    List<MaterialBrand> findAllActiveOrderByQualityAndName();

    @Query("SELECT mb FROM MaterialBrand mb WHERE mb.qualityRating >= :minRating AND mb.isActive = true")
    List<MaterialBrand> findByMinQualityRating(@Param("minRating") Integer minRating);
}
