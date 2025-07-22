package it.aredegalli.printer.config;

import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final LogService logService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDatabase() {
        try {
            logService.info("DatabaseInitializer", "Initializing database views...");

            // Create views if they don't exist
            createQueuedJobsView();
            createRunningJobsView();

            logService.info("DatabaseInitializer", "Database views initialized successfully");

        } catch (Exception e) {
            logService.error("DatabaseInitializer", "Failed to initialize database: " + e.getMessage());
            // Don't rethrow - let the application continue
        }
    }

    private void createQueuedJobsView() {
        try {
            String sql = """
                    CREATE OR REPLACE VIEW queued_jobs_per_printer AS
                    SELECT 
                        p.id as printer_id,
                        j.id as job_id
                    FROM printer p
                    LEFT JOIN job j ON j.printer_id = p.id 
                    WHERE j.status = 'QUE'
                    AND j.id IS NOT NULL
                    """;

            jdbcTemplate.execute(sql);
            logService.debug("DatabaseInitializer", "Created queued_jobs_per_printer view");

        } catch (Exception e) {
            logService.warn("DatabaseInitializer", "Failed to create queued_jobs_per_printer view: " + e.getMessage());
        }
    }

    private void createRunningJobsView() {
        try {
            String sql = """
                    CREATE OR REPLACE VIEW running_jobs_per_printer AS
                    SELECT 
                        p.id as printer_id,
                        j.id as job_id
                    FROM printer p
                    LEFT JOIN job j ON j.printer_id = p.id 
                    WHERE j.status = 'RUN'
                    AND j.id IS NOT NULL
                    """;

            jdbcTemplate.execute(sql);
            logService.debug("DatabaseInitializer", "Created running_jobs_per_printer view");

        } catch (Exception e) {
            logService.warn("DatabaseInitializer", "Failed to create running_jobs_per_printer view: " + e.getMessage());
        }
    }
}
