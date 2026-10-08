package com.gymflow.booking;

import com.gymflow.booking.dto.BookSessionRequest;
import com.gymflow.booking.dto.MyBookingResponse;
import com.gymflow.common.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reservas del socio autenticado (LOGIC.md §6: /me/bookings). */
@RestController
@RequestMapping("/api/v1/me/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final ScheduleService scheduleService;

    public BookingController(BookingService bookingService, ScheduleService scheduleService) {
        this.bookingService = bookingService;
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<MyBookingResponse> myBookings(Authentication authentication) {
        return scheduleService.listForUser(TenantContext.require(), userId(authentication));
    }

    @PostMapping
    public ResponseEntity<Void> book(Authentication authentication, @Valid @RequestBody BookSessionRequest request) {
        Booking booking = bookingService.book(TenantContext.require(), userId(authentication), request.sessionId());
        HttpStatus status = booking.getStatus() == BookingStatus.WAITLIST ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable UUID id) {
        bookingService.cancel(TenantContext.require(), userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
