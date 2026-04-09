package com.agora.service.reservation;

import com.agora.exception.BusinessException;
import com.agora.exception.ErrorCode;
import com.agora.repository.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Attribution d'une référence unique {@code yyMMdd + 5 chiffres} par date de réservation.
 */
@Service
@RequiredArgsConstructor
public class ReservationBookingReferenceService {

    private static final DateTimeFormatter PREFIX_FMT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int MAX_ATTEMPTS = 20;

    private final ReservationRepository reservationRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public String allocateNextReference(LocalDate reservationDate) {
        String prefix = reservationDate.format(PREFIX_FMT);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int maxSeq = reservationRepository.findMaxSequenceForDatePrefix(prefix);
            int next = maxSeq + 1;
            if (next > 99_999) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "Numérotation des réservations épuisée pour cette date."
                );
            }
            String candidate = prefix + String.format("%05d", next);
            if (!reservationRepository.existsByBookingReference(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(
                ErrorCode.VALIDATION_ERROR,
                "Impossible d'attribuer une référence de réservation."
        );
    }
}
