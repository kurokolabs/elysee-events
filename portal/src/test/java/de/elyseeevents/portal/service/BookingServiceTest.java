package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.Booking;
import de.elyseeevents.portal.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void saveSetsDefaultStatusAnfrageWhenNull() {
        Booking booking = new Booking();
        booking.setStatus(null);

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.save(booking);

        assertEquals("ANFRAGE", result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void savePreservesExplicitStatus() {
        Booking booking = new Booking();
        booking.setStatus("BESTAETIGT");

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.save(booking);

        assertEquals("BESTAETIGT", result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void findByIdDelegatesToRepository() {
        Booking booking = new Booking();
        booking.setId(42L);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));

        Optional<Booking> result = bookingService.findById(42L);

        assertTrue(result.isPresent());
        assertEquals(42L, result.get().getId());
        verify(bookingRepository).findById(42L);
    }

    @Test
    void findByCustomerIdDelegatesToRepository() {
        Booking booking = new Booking();
        booking.setCustomerId(10L);
        when(bookingRepository.findByCustomerId(10L)).thenReturn(List.of(booking));

        List<Booking> result = bookingService.findByCustomerId(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getCustomerId());
        verify(bookingRepository).findByCustomerId(10L);
    }

    @Test
    void updateStatusDelegatesToRepository() {
        bookingService.updateStatus(5L, "STORNIERT");

        verify(bookingRepository).updateStatus(5L, "STORNIERT");
    }
}
