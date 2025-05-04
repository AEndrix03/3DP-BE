package it.aredegalli.printer.repository.log;

import it.aredegalli.printer.model.log.LogLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogLevelRepository extends JpaRepository<LogLevel, String> {

}
