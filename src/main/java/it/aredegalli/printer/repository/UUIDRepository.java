package it.aredegalli.printer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

@NoRepositoryBean
public interface UUIDRepository<T> extends JpaRepository<T, UUID> {
}
