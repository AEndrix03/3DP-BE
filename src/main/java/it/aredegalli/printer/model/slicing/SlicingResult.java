package it.aredegalli.printer.model.slicing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "slicing_result")
public class SlicingResult {

    @Id
    @Column(updatable = false, nullable = false)
    private byte[] id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "hash", nullable = false)
    private FileResource generatedFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_hash", referencedColumnName = "hash", nullable = false)
    private FileResource sourceFile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @Column(columnDefinition = "TEXT")
    private String logs;

    private Instant createdAt = Instant.now();

    private Long lines;
}
