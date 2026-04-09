package com.agora.repository.waitlist;

import com.agora.entity.waitlist.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {

    void deleteByUser_Id(UUID userId);

    @Query("""
            select w from WaitlistEntry w
            join fetch w.resource r
            where w.user.id = :userId
            order by w.createdAt desc
            """)
    List<WaitlistEntry> findAllByUserIdWithResource(@Param("userId") UUID userId);

    boolean existsByUser_IdAndResource_IdAndSlotDateAndSlotStartAndSlotEnd(
            UUID userId,
            UUID resourceId,
            LocalDate slotDate,
            String slotStart,
            String slotEnd
    );

    @Query("""
            select coalesce(max(w.position), 0) from WaitlistEntry w
            where w.resource.id = :resourceId
              and w.slotDate = :slotDate
              and w.slotStart = :slotStart
              and w.slotEnd = :slotEnd
              and w.status = :waitingStatus
            """)
    int maxPositionForSlot(
            @Param("resourceId") UUID resourceId,
            @Param("slotDate") LocalDate slotDate,
            @Param("slotStart") String slotStart,
            @Param("slotEnd") String slotEnd,
            @Param("waitingStatus") com.agora.enums.waitlist.WaitlistStatus waitingStatus
    );

    Optional<WaitlistEntry> findByIdAndUser_Id(UUID id, UUID userId);
}
