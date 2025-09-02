package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDto;
import com.project.airBnbApp.dto.BookingRequest;
import com.project.airBnbApp.dto.GuestDto;
import com.stripe.model.Event;

import java.util.List;

public interface BookingService {
    public BookingDto intialiseBooking(BookingRequest bookingRequest);


    BookingDto addGuests(long bookingId, List<GuestDto> guestDtoList);

    String initiatePayments(Long bookingId);

    void capturePayment(Event event);

    void cancelPayments(Long bookingId);

    String getBookingStatus(Long bookingId);
}
