package com.project.airBnbApp.controller;

import com.project.airBnbApp.service.BookingService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook")
public class WebhookController {

    private final BookingService bookingService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;
    @PostMapping("/payment")
    @Operation(summary = "Capture the payments", tags = {"Webhook"})
    public ResponseEntity<Void> capturePayments(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader){
        try{
            Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            if(event==null){
                System.out.println(event.toString());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            bookingService.capturePayment(event);
            return ResponseEntity.ok().build();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
