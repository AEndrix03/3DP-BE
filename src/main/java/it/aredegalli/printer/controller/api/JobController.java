package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.job.request.JobStartRequestDto;
import it.aredegalli.printer.service.job.JobService;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final LogService logService;

    @PostMapping("/start")
    public ResponseEntity<UUID> startJob(@RequestBody JobStartRequestDto jobStartRequestDto) {
        this.logService.info("JobController", "Starting job with request: " + jobStartRequestDto.toString());
        return ResponseEntity.ok(jobService.startJob(jobStartRequestDto));
    }

}
