package com.zaheudev.gateway.controller;

import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.dto.RefundRequest;
import com.zaheudev.gateway.exception.PaymentFailedException;
import com.zaheudev.gateway.model.Amount;
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

    @PostMapping("api/v1/capture/{paymentId}")
    public ResponseEntity<PaymentResponse> capturePayment(@PathVariable String paymentId){
        try{
            PaymentResponse response = paymentService.capturePayment(paymentId);
            return new ResponseEntity<>(response, null, 202);
        }catch (PaymentFailedException e){
            return paymentService.getResponseEntity(500, e.getPaymentEntity());
        }
    }

    @PostMapping("api/v1/refund/{paymentId}")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String paymentId, @RequestBody RefundRequest amount){
        try{
            PaymentResponse response = paymentService.refundPayment(paymentId, amount);
            return new ResponseEntity<>(response, null, 202);
        }catch(PaymentFailedException e){
            return paymentService.getResponseEntity(500, e.getPaymentEntity());
        }
    }

    @PostMapping(value = "api/v1/void/{paymentId}")
    public ResponseEntity<PaymentResponse> voidPayment(@PathVariable String paymentId){
            try{
                PaymentResponse response = paymentService.voidPayment(paymentId);
                return new ResponseEntity<>(response, null, 202);
            }catch (PaymentFailedException e){
                return paymentService.getResponseEntity(500, e.getPaymentEntity());
            }
    }

    @GetMapping(value = "/api/v1/payments")
    public ResponseEntity<List<PaymentResponse>> getAllPayments(){
        try{
            return new ResponseEntity<>(paymentService.getAllPayments(), null, 200);
        }catch (Exception e){
            return new ResponseEntity<>(null, null, 500);
        }
    }

    @GetMapping(value = "/api/v1/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId){
        try{
            return new ResponseEntity<>(paymentService.getPayment(paymentId), null, 200);
        } catch (PaymentFailedException e) {
            return paymentService.getResponseEntity(404, e.getPaymentEntity());
        }
    }

}
