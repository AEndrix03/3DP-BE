package it.aredegalli.printer.mapper.job;

import it.aredegalli.printer.dto.job.JobDto;
import it.aredegalli.printer.model.job.Job;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobMapper {

    JobDto toDto(Job job);

    List<JobDto> toDto(List<Job> jobs);

    @AfterMapping
    default void mapTotalLines(Job job, @MappingTarget JobDto dto) {
        /*if (job.getFileResource() != null && job.getFileResource().getContent() != null) {
            dto.setTotalLines(job.getFileResource().getTotalLines());
        }*/
    }
}
