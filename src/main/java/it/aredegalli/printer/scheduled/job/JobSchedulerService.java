package it.aredegalli.printer.scheduled.job;

import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.model.view.QueuedJobsPerPrinter;
import it.aredegalli.printer.model.view.RunningJobsPerPrinter;
import it.aredegalli.printer.repository.view.QueuedJobsPerPrinterRepository;
import it.aredegalli.printer.repository.view.RunningJobsPerPrinterRepository;
import it.aredegalli.printer.service.job.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class JobSchedulerService {

    private final QueuedJobsPerPrinterRepository queuedJobsPerPrinterRepository;
    private final RunningJobsPerPrinterRepository runningJobsPerPrinterRepository;
    private final JobService jobService;

    @Scheduled(fixedDelay = 60000)
    public void scheduleJobExecution() {
        log.debug("JobSchedulerService - Checking for queued jobs to run");
        handleQueuedJobs();
    }
    
    private void handleQueuedJobs() {
        Map<UUID, Job> queuedJobs = queuedJobsPerPrinterRepository.findAll().stream()
                .collect(Collectors.toMap(QueuedJobsPerPrinter::getPrinterId, QueuedJobsPerPrinter::getJob));
        Set<UUID> runningJobs = runningJobsPerPrinterRepository.findAll().stream()
                .map(RunningJobsPerPrinter::getPrinterId)
                .collect(Collectors.toSet());

        for (Map.Entry<UUID, Job> entry : queuedJobs.entrySet()) {
            if (!runningJobs.contains(entry.getKey())) {
                jobService.runJob(entry.getValue().getId());
            }
        }
    }

}
