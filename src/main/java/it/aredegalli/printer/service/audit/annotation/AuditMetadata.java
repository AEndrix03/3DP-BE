package it.aredegalli.printer.service.audit.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AuditMetadataContainer.class)
@Documented
public @interface AuditMetadata {
    /**
     * The key for the metadata entry.
     *
     * @return the key as a String
     */
    String key();

    /**
     * The value for the metadata entry.
     *
     * @return the value as a String
     */
    String value();
}
