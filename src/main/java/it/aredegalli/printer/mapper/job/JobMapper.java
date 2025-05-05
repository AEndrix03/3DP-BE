package it.aredegalli.printer.mapper.job;

import it.aredegalli.printer.dto.job.JobDto;
import it.aredegalli.printer.model.job.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "printerId", source = "printer.id")
    @Mapping(target = "resourceId", source = "fileResource.id")
    @Mapping(target = "totalLines", source = "fileResource.getTotalLines")
    @Mapping(target = "statusCode", source = "status.code")
    JobDto toDto(Job job);

    List<JobDto> toDto(List<Job> jobs);
}
