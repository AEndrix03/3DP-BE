package it.aredegalli.printer.service.resource;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ResourceSecureDownloadHelper {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:300}")
    private int tokenExpirationSeconds;

    private Algorithm algorithm;

    @PostConstruct
    private void initAlgorithm() {
        this.algorithm = Algorithm.HMAC256(jwtSecret);
    }

    /**
     * Genera un token di download sicuro per una risorsa specifica
     */
    public String generateSecureDownloadToken(String resourceId, String driverId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + (tokenExpirationSeconds * 1000L));

        return JWT.create()
                .withIssuer("secure-download-service")
                .withSubject(driverId)
                .withClaim("resourceId", resourceId)
                .withClaim("downloadPurpose", "secure-resource-access")
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    /**
     * Valida il token e restituisce i dati della risorsa
     */
    public String validateTokenAndExtractResourceId(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("secure-download-service")
                    .withClaim("downloadPurpose", "secure-resource-access")
                    .build();

            DecodedJWT jwt = verifier.verify(token);

            return jwt.getClaim("resourceId").asString();

        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
