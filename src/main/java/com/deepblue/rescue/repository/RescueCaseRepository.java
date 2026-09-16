package com.deepblue.rescue.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {
    Optional<RescueCase> findByCaseCode(String caseCode);
    List<RescueCase> findByStatus(RescueStatus status);
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);
    List<RescueCase> findByRescueCenterCode(String code);
    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate rescueDate);
}