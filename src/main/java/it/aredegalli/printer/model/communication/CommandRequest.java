package it.aredegalli.printer.model.communication;

import it.aredegalli.printer.model.printer.Printer;
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
@Table(name = "command_request")
public class CommandRequest {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "printer_id")
    private Printer printer;

    @Column(columnDefinition = "jsonb")
    private String content;

    private Instant submittedAt;
    private Boolean executed;
}
