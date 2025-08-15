package it.aredegalli.printer.service.job;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.job.JobDto;
import it.aredegalli.printer.dto.job.request.JobStartRequestDto;
import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.mapper.job.JobMapper;
import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.model.slicing.result.SlicingResult;
import it.aredegalli.printer.repository.job.JobRepository;
import it.aredegalli.printer.repository.printer.PrinterRepository;
import it.aredegalli.printer.repository.slicing.result.SlicingResultRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final PrinterRepository printerRepository;
    private final SlicingResultRepository slicingResultRepository;

    @Override
    public JobDto getJob(UUID id) {
        return jobRepository.findById(id).stream()
                .map(jobMapper::toDto)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Job not found"));
    }

    @Override
    public List<JobDto> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(jobMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UUID startJob(JobStartRequestDto startRequestDto) {
        Printer printer = printerRepository.findById(startRequestDto.getPrinterId())
                .orElseThrow(() -> new NotFoundException("Printer not found"));
        SlicingResult slicingResult = slicingResultRepository.findById(startRequestDto.getSlicingId())
                .orElseThrow(() -> new NotFoundException("Slicing result not found"));

        Job job = Job.builder()
                .printer(printer)
                .status(JobStatusEnum.CREATED)
                .slicingResult(slicingResult)
                .startOffsetLine(startRequestDto.getStartOffset())
                .createdAt(Instant.now())
                .build();

        this.jobRepository.save(job);
        return job.getId();
    }

}
