package it.aredegalli.printer.scheduled.job;

import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.repository.job.JobRepository;
import it.aredegalli.printer.service.kafka.control.check.PrinterCheckService;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class JobCheckSchedulerService {

    private final JobRepository jobRepository;
    private final PrinterCheckService printerCheckService;
    private final LogService logService;

    @Scheduled(fixedDelay = 30000) // 30 seconds
    public void scheduleJobCheckExecution() {
        try {
            List<Job> runningJobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatusEnum.RUNNING);

            if (!runningJobs.isEmpty()) {
                for (Job job : runningJobs) {
                    logService.info("JobCheckSchedulerService",
                            String.format("Request sent to Driver: %s", job.getPrinter().getDriverId()));
                    this.printerCheckService.checkPrinter(job.getPrinter().getDriverId(), job.getId(), null);
                }
            }
        } catch (Exception e) {
            logService.error("JobCheckSchedulerService",
                    "Error during job check scheduling: " + e.getMessage());
        }
    }

}
