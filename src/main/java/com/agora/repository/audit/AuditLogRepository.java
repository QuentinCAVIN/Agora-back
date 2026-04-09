package com.agora.repository.audit;

import com.agora.entity.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findAllByOrderByPerformedAtDesc(Pageable pageable);

    /**
     * Filtre JSON PostgreSQL (colonne {@code details} jsonb). Non exécuté sur H2 sans jsonb → voir service.
     */
    @Query(
            value = "SELECT * FROM audit_logs WHERE details ->> 'reservationId' = :reservationId ORDER BY performed_at DESC",
            countQuery = "SELECT count(*) FROM audit_logs WHERE details ->> 'reservationId' = :reservationId",
            nativeQuery = true)
    Page<AuditLog> findPageByDetailsReservationId(
            @Param("reservationId") String reservationId, Pageable pageable);
}
