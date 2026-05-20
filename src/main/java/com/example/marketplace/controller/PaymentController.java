package com.example.marketplace.controller;

import com.example.marketplace.dto.PaymentDtos.PaymentResponse;
import com.example.marketplace.dto.PaymentDtos.SimulatePaymentRequest;
import com.example.marketplace.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/simulate")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse simulate(@Valid @RequestBody SimulatePaymentRequest request) {
        return paymentService.simulate(request);
    }
}
