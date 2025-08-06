package it.aredegalli.printer.config;

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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeDatabase() {
        try {
            createQueuedJobsView();
            createRunningJobsView();
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
        }
    }
}
