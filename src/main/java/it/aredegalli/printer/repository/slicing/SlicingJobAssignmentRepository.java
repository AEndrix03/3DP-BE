package it.aredegalli.printer.repository.slicing;

import it.aredegalli.printer.model.slicing.SlicingJobAssignment;
import it.aredegalli.printer.model.slicing.SlicingJobAssignmentStatus;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlicingJobAssignmentRepository extends UUIDRepository<SlicingJobAssignment> {

    List<SlicingJobAssignment> findByContainerId(UUID containerId);

    List<SlicingJobAssignment> findByAssignmentStatus(SlicingJobAssignmentStatus status);

    Optional<SlicingJobAssignment> findBySlicingQueueIdAndAssignmentStatusIn(
            UUID slicingQueueId, List<SlicingJobAssignmentStatus> statuses);

    long countByContainerIdAndAssignmentStatus(UUID containerId, SlicingJobAssignmentStatus status);
}