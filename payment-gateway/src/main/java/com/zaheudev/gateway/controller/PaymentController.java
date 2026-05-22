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
            return ResponseEntity.status(500).body(PaymentResponse.builder()
                    .paymentId(e.getPaymentEntity().getPaymentId())
                    .rrn(e.getPaymentEntity().getRrn())
                    .authCode(e.getPaymentEntity().getAuthCode())
                    .processorTransactionId(e.getPaymentEntity().getProcessorTransactionId())
                    .message(e.getPaymentEntity().getErrorMessage())
                    .paymentStatus(e.getPaymentEntity().getStatus())
                    .createdAt(e.getPaymentEntity().getCreatedAt())
                    .amount(Amount.builder()
                            .amount(e.getPaymentEntity().getAmount().longValue())
                            .currency(e.getPaymentEntity().getCurrency())
                            .build())
                    .build());
        }
    }

    @PostMapping("api/v1/refund/{paymentId}")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String paymentId, @RequestBody RefundRequest amount){
        try{
            PaymentResponse response = paymentService.refundPayment(paymentId, amount);
            return new ResponseEntity<>(response, null, 202);
        }catch(PaymentFailedException e){
            return ResponseEntity.status(500).body(PaymentResponse.builder()
                    .paymentId(e.getPaymentEntity().getPaymentId())
                    .rrn(e.getPaymentEntity().getRrn())
                    .authCode(e.getPaymentEntity().getAuthCode())
                    .processorTransactionId(e.getPaymentEntity().getProcessorTransactionId())
                    .message(e.getPaymentEntity().getErrorMessage())
                    .paymentStatus(e.getPaymentEntity().getStatus())
                    .createdAt(e.getPaymentEntity().getCreatedAt())
                    .message(e.getPaymentEntity().getErrorMessage())
                    .amount(Amount.builder()
                            .amount(e.getPaymentEntity().getAmount().longValue())
                            .currency(e.getPaymentEntity().getCurrency())
                            .build())
                    .build());
        }
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
