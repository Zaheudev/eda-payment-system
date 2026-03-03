package com.zaheudev.gateway.controller;

import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.model.Payment;
import com.zaheudev.gateway.service.PaymentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PaymentController {
    @Autowired
    PaymentServiceImpl paymentService;

    @PostMapping("/api/v1/payments")
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody CreatePaymentRequest request){
        PaymentResponse response = paymentService.createPayment(request);
        return new ResponseEntity<>(response, null, 201);
    }

    @GetMapping(value = "/api/v1/payments")
    public List<Payment> getAllPayments(){
        return null;
    }

    @GetMapping(value = "/api/v1/payments/{paymentId}")
    public Payment getPayment(@PathVariable String paymentId){
        return null;
    }

}
