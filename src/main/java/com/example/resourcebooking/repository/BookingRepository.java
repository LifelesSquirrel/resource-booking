package com.example.resourcebooking.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.resourcebooking.entity.Booking;
import com.example.resourcebooking.entity.BookingStatus;

import java.time.LocalDateTime;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.resource.id = :resourceId
              and b.status <> :cancelledStatus
              and :startTime < b.endTime
              and :endTime > b.startTime
            """)
    boolean existsConflict(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cancelledStatus") BookingStatus cancelledStatus
    );
}