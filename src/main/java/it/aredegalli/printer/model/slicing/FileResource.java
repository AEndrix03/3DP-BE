package it.aredegalli.printer.model.slicing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "file_resource", uniqueConstraints = {
        @UniqueConstraint(columnNames = "file_hash")
})
public class FileResource {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 255)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "file_hash", nullable = false, unique = true)
    private byte[] fileHash;

    @Column(name = "uploaded_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private Instant uploadedAt;

    @Column(name = "bucket_name", nullable = false)
    @Builder.Default
    private String bucketName = "default";

    @Column(name = "object_key", nullable = false)
    private String objectKey;
}
