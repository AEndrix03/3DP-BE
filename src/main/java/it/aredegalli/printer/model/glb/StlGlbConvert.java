package it.aredegalli.printer.model.glb;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stl_glb_convert")
public class StlGlbConvert {

    @Id
    @Column(name = "stl")
    private String stlHash;

    @Column(name = "glb")
    private String glbHash;

}
