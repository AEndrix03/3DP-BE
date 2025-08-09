package it.aredegalli.printer.repository.material;

import it.aredegalli.printer.model.material.Material;
import it.aredegalli.printer.model.material.MaterialBrand;
import it.aredegalli.printer.model.material.MaterialType;
import it.aredegalli.printer.repository.UUIDRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialRepository extends UUIDRepository<Material> {

    List<Material> findByType(MaterialType type);

    List<Material> findByBrand(MaterialBrand brand);

    List<Material> findByTypeAndBrand(MaterialType type, MaterialBrand brand);

    @Query("SELECT m FROM Material m JOIN FETCH m.type JOIN FETCH m.brand")
    List<Material> findAllWithTypeAndBrand();

    @Query("SELECT m FROM Material m JOIN FETCH m.type JOIN FETCH m.brand WHERE LOWER(m.type.name) = LOWER(:typeName)")
    List<Material> findByTypeNameWithRelations(@Param("typeName") String typeName);

    @Query("SELECT m FROM Material m JOIN FETCH m.type JOIN FETCH m.brand WHERE LOWER(m.brand.name) = LOWER(:brandName)")
    List<Material> findByBrandNameWithRelations(@Param("brandName") String brandName);

    @Query("SELECT DISTINCT m FROM Material m " +
            "JOIN FETCH m.type t " +
            "JOIN FETCH m.brand b " +
            "WHERE (:name IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:type IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :type, '%'))) " +
            "AND (:brand IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :brand, '%'))) " +
            "ORDER BY m.name ASC")
    List<Material> searchMaterials(@Param("name") String name,
                                   @Param("type") String type,
                                   @Param("brand") String brand);
}