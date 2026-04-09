package com.agora.repository.calendar;

import com.agora.entity.calendar.BlackoutPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BlackoutPeriodRepository extends JpaRepository<BlackoutPeriod, UUID> {

    /**
     * Fermeture globale ({@code resource} nulle) ou ciblée sur la ressource, intersectant {@code date}.
     */
    @Query("""
            SELECT COUNT(b) FROM BlackoutPeriod b
            WHERE b.dateFrom <= :date AND b.dateTo >= :date
            AND (b.resource IS NULL OR b.resource.id = :resourceId)
            """)
    long countBlockingForResourceOnDate(@Param("resourceId") UUID resourceId, @Param("date") LocalDate date);

    @Query("""
            SELECT b FROM BlackoutPeriod b
            LEFT JOIN FETCH b.resource
            WHERE b.dateFrom <= :end AND b.dateTo >= :start
            """)
    List<BlackoutPeriod> findOverlappingRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
