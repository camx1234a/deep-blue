package com.deepblue.rescue.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deepblue.rescue.domain.Treatment;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    
    // Query Method simple
    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    // JPQL por intervalo de fechas
    @Query("""
        select t
        from Treatment t
        where t.performedAt between :start and :end
        order by t.performedAt asc
        """)
    List<Treatment> findByPerformedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // JPQL navegando tres entidades (Treatment -> Animal -> RescueCase -> RescueCenter)
    @Query("""
        select t
        from Treatment t
        join t.animal a
        join a.rescueCase rc
        join rc.rescueCenter rcent
        where rcent.code = :centerCode
        """)
    List<Treatment> findByCenterCode(@Param("centerCode") String centerCode);

    @Query("""
        select distinct t
        from Treatment t
        join t.specialist s
        join s.expertiseAreas e
        where lower(e.name) = lower(:expertiseName)
        """)
    List<Treatment> findBySpecialistExpertiseName(@Param("expertiseName") String expertiseName);
    
}