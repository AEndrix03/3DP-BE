package it.aredegalli.printer.model.jwt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "revoked_token")
public class RevokedToken {
    @Id
    @Column(name = "jti")
    private String jti;

    @Column(name = "user_ref")
    private String userRef;

    private Instant revokedAt;
    private Instant expiresAt;
}
