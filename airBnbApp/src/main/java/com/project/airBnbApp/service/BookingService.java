package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.BookingDto;
import com.project.airBnbApp.dto.BookingRequest;
import com.project.airBnbApp.dto.GuestDto;

import java.util.List;

public interface BookingService {
    public BookingDto intialiseBooking(BookingRequest bookingRequest);


    BookingDto addGuests(long bookingId, List<GuestDto> guestDtoList);
}
