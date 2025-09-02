package com.project.airBnbApp.service;

import com.project.airBnbApp.entity.Booking;

public interface CheckOutService {
    String getCheckoutSession(Booking booking, String successUrl, String failureUrl);

}
