package it.aredegalli.printer.mapper.job;

import it.aredegalli.printer.dto.job.JobStatusDto;
import it.aredegalli.printer.model.job.JobStatus;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobStatusMapper {

    JobStatusDto toDto(JobStatus jobStatus);

    List<JobStatusDto> toDto(List<JobStatus> jobStatus);
}
