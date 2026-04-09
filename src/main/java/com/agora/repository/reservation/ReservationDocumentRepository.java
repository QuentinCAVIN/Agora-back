package com.agora.repository.reservation;

import com.agora.entity.reservation.ReservationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationDocumentRepository extends JpaRepository<ReservationDocument, UUID> {

    void deleteByReservation_User_Id(UUID userId);

    List<ReservationDocument> findByReservation_IdOrderByCreatedAtDesc(UUID reservationId);
}
