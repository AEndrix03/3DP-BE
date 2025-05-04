package it.aredegalli.printer.service.log;

import java.util.Map;

public interface LogService {
    void info(String logger, String message);

    void info(String logger, String message, Map<String, Object> context);

    void debug(String logger, String message);

    void debug(String logger, String message, Map<String, Object> context);

    void warn(String logger, String message);

    void warn(String logger, String message, Map<String, Object> context);

    void error(String logger, String message);

    void error(String logger, String message, Map<String, Object> context);

    void fatal(String logger, String message);

    void fatal(String logger, String message, Map<String, Object> context);
}
