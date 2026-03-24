package com.example.resourcebooking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import com.example.resourcebooking.dto.CreateBookingRequest;
import com.example.resourcebooking.entity.Booking;
import com.example.resourcebooking.entity.BookingStatus;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.repository.BookingRepository;
import com.example.resourcebooking.repository.ResourceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private BookingService bookingService;

    private Resource createActiveResource(Long id) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setName("Meeting A");
        resource.setType("ROOM");
        resource.setLocation("3 floor");
        resource.setDescription("Room for 8 people");
        resource.setActive(true);
        return resource;
    }

    private CreateBookingRequest createRequest() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setResourceId(1L);
        request.setBookedBy("Ivan Ivanov");
        request.setStartTime(LocalDateTime.of(2025, 4, 20, 10, 0));
        request.setEndTime(LocalDateTime.of(2025, 4, 20, 11, 0));
        request.setPurpose("Project meeting");
        return request;
    }

    @Test
    void createBooking_shouldSaveBooking_whenDataIsValid() {
        Resource resource = createActiveResource(1L);
        CreateBookingRequest request = createRequest();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(bookingRepository.existsConflict(
                eq(1L),
                eq(request.getStartTime()),
                eq(request.getEndTime()),
                eq(BookingStatus.CANCELLED)
        )).thenReturn(false);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.createBooking(request);

        assertNotNull(result);
        assertEquals("Ivan Ivanov", result.getBookedBy());
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(resource, result.getResource());

        verify(resourceRepository).findById(1L);
        verify(bookingRepository).existsConflict(
                1L,
                request.getStartTime(),
                request.getEndTime(),
                BookingStatus.CANCELLED
        );
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_shouldThrow404_whenResourceNotFound() {
        CreateBookingRequest request = createRequest();

        when(resourceRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals(404, ex.getStatusCode().value());
        verify(resourceRepository).findById(1L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrow400_whenStartTimeIsAfterEndTime() {
        Resource resource = createActiveResource(1L);
        CreateBookingRequest request = createRequest();
        request.setStartTime(LocalDateTime.of(2025, 4, 20, 12, 0));
        request.setEndTime(LocalDateTime.of(2025, 4, 20, 11, 0));

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals(400, ex.getStatusCode().value());
        verify(resourceRepository).findById(1L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrow409_whenConflictExists() {
        Resource resource = createActiveResource(1L);
        CreateBookingRequest request = createRequest();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(bookingRepository.existsConflict(
                eq(1L),
                eq(request.getStartTime()),
                eq(request.getEndTime()),
                eq(BookingStatus.CANCELLED)
        )).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals(409, ex.getStatusCode().value());
        verify(resourceRepository).findById(1L);
        verify(bookingRepository).existsConflict(
                1L,
                request.getStartTime(),
                request.getEndTime(),
                BookingStatus.CANCELLED
        );
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_shouldSetStatusCancelled_whenBookingExists() {
        Resource resource = createActiveResource(1L);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setResource(resource);
        booking.setBookedBy("Ivan Ivanov");
        booking.setStartTime(LocalDateTime.of(2025, 4, 20, 10, 0));
        booking.setEndTime(LocalDateTime.of(2025, 4, 20, 11, 0));
        booking.setPurpose("Project meeting");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(1L);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(bookingRepository).findById(1L);
        verify(bookingRepository).save(booking);
    }

    @Test
    void getAllBookings_shouldReturnList() {
        when(bookingRepository.findAll()).thenReturn(List.of(new Booking(), new Booking()));

        List<Booking> result = bookingService.getAllBookings();

        assertEquals(2, result.size());
        verify(bookingRepository).findAll();
    }
}