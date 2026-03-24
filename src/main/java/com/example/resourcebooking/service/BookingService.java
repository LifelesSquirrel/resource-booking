package com.example.resourcebooking.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.example.resourcebooking.dto.CreateBookingRequest;
import com.example.resourcebooking.entity.Booking;
import com.example.resourcebooking.entity.BookingStatus;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.repository.BookingRepository;
import com.example.resourcebooking.repository.ResourceRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;

    public BookingService(BookingRepository bookingRepository, ResourceRepository resourceRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
    }

    public Booking createBooking(CreateBookingRequest request) {
        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ресурс не найден"));

        if (!resource.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ресурс недоступен для бронирования");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Время начала должно быть раньше времени окончания");
        }

        boolean conflict = bookingRepository.existsConflict(
                resource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                BookingStatus.CANCELLED
        );

        if (conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ресурс уже забронирован на этот период");
        }

        Booking booking = new Booking();
        booking.setResource(resource);
        booking.setBookedBy(request.getBookedBy());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бронирование не найдено"));

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}