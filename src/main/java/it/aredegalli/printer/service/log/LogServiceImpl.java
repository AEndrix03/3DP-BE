package it.aredegalli.printer.service.log;

import it.aredegalli.printer.enums.log.LogLevelEnum;
import it.aredegalli.printer.model.log.LogEntry;
import it.aredegalli.printer.model.log.LogLevel;
import it.aredegalli.printer.repository.log.LogEntryRepository;
import it.aredegalli.printer.repository.log.LogLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LogEntryRepository logEntryRepository;
    private final LogLevelRepository logLevelRepository;

    @Async
    @Override
    public void info(String logger, String message) {
        log(LogLevelEnum.INFO, logger, message, null);
    }

    @Async
    @Override
    public void info(String logger, String message, Map<String, Object> context) {
        log(LogLevelEnum.INFO, logger, message, context);
    }

    @Async
    @Override
    public void debug(String logger, String message) {
        log(LogLevelEnum.DEBUG, logger, message, null);
    }

    @Async
    @Override
    public void debug(String logger, String message, Map<String, Object> context) {
        log(LogLevelEnum.DEBUG, logger, message, context);
    }

    @Async
    @Override
    public void warn(String logger, String message) {
        log(LogLevelEnum.WARN, logger, message, null);
    }

    @Async
    @Override
    public void warn(String logger, String message, Map<String, Object> context) {
        log(LogLevelEnum.WARN, logger, message, context);
    }

    @Async
    @Override
    public void error(String logger, String message) {
        log(LogLevelEnum.ERROR, logger, message, null);
    }

    @Async
    @Override
    public void error(String logger, String message, Map<String, Object> context) {
        log(LogLevelEnum.ERROR, logger, message, context);
    }

    @Async
    @Override
    public void fatal(String logger, String message) {
        log(LogLevelEnum.FATAL, logger, message, null);
    }

    @Async
    @Override
    public void fatal(String logger, String message, Map<String, Object> context) {
        log(LogLevelEnum.FATAL, logger, message, context);
    }
    
    private void log(LogLevelEnum level, String logger, String message, Map<String, Object> context) {
        LogLevel _level = logLevelRepository.findById(level.getCode()).orElse(null);

        assert _level != null;

        LogEntry entry = LogEntry.builder()
                .level(_level)
                .timestamp(Instant.now())
                .logger(logger)
                .message(message)
                .context(context)
                .build();


        log.debug("[Logger] Logging: {}", entry);

        logEntryRepository.save(entry);
    }

}
