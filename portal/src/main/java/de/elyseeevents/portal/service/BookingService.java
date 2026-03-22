package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.Booking;
import de.elyseeevents.portal.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public List<Booking> findByFilters(String type, String status, String dateFrom, String dateTo) {
        return bookingRepository.findByFilters(type, status, dateFrom, dateTo);
    }

    public List<Booking> findByCustomerId(Long customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public Booking save(Booking booking) {
        if (booking.getStatus() == null) {
            booking.setStatus("ANFRAGE");
        }
        return bookingRepository.save(booking);
    }

    public void updateStatus(Long id, String status) {
        bookingRepository.updateStatus(id, status);
    }

    public java.util.List<java.util.Map<String, Object>> monthlyRevenue(int months) {
        return bookingRepository.monthlyRevenue(months);
    }

    public long count() {
        return bookingRepository.count();
    }

    public long countByStatus(String status) {
        return bookingRepository.countByStatus(status);
    }

    public long countThisMonth() {
        return bookingRepository.countThisMonth();
    }

    public Double totalBudget() {
        return bookingRepository.totalBudget();
    }

    public List<Booking> findRecent(int limit) {
        return bookingRepository.findRecent(limit);
    }

    public java.util.List<java.util.Map<String, Object>> availabilityData(int year, int month) {
        return bookingRepository.availabilityData(year, month);
    }

    public java.util.List<java.util.Map<String, Object>> calendarData(int year, int month) {
        return bookingRepository.calendarData(year, month);
    }
}
