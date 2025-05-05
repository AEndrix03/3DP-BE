package it.aredegalli.printer.service.job.status;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.job.JobStatusDto;
import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.mapper.job.JobStatusMapper;
import it.aredegalli.printer.model.job.JobStatus;
import it.aredegalli.printer.repository.job.JobStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobStatusServiceImpl implements JobStatusService {

    private final JobStatusRepository jobStatusRepository;
    private final JobStatusMapper jobStatusMapper;

    @Override
    public JobStatusDto getJobStatusByCode(String code) {
        return jobStatusMapper.toDto(jobStatusRepository.findById(code).orElseThrow());
    }

    @Override
    public JobStatusDto getJobStatus(JobStatusEnum jobStatusEnum) {
        return getJobStatusByCode(jobStatusEnum.getCode());
    }

    @Override
    public JobStatus status(JobStatusEnum jobStatusEnum) {
        return jobStatusRepository.findById(jobStatusEnum.getCode()).orElseThrow(() -> new NotFoundException("JobStatus not found"));
    }

}
