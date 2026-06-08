package com.zaheudev.gateway.controller;

import com.zaheudev.gateway.dto.CreatePaymentRequest;
import com.zaheudev.gateway.dto.PaymentResponse;
import com.zaheudev.gateway.dto.RefundRequest;
import com.zaheudev.gateway.entity.PaymentEntity;
import com.zaheudev.gateway.exception.PaymentFailedException;
import com.zaheudev.gateway.service.PaymentServiceImpl;
import com.zaheudev.shared.dto.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentServiceImpl paymentService;

    @InjectMocks
    private PaymentController controller;

    @Test
    void createPaymentShouldReturn201() {
        PaymentResponse response = PaymentResponse.builder().paymentId("PMT001").build();
        when(paymentService.createPayment(any())).thenReturn(response);

        ResponseEntity<PaymentResponse> result = controller.createPayment(new CreatePaymentRequest());

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void capturePaymentShouldReturn202() {
        PaymentResponse response = PaymentResponse.builder().paymentId("PMT001").build();
        when(paymentService.capturePayment("PMT001")).thenReturn(response);

        ResponseEntity<PaymentResponse> result = controller.capturePayment("PMT001");

        assertThat(result.getStatusCode().value()).isEqualTo(202);
    }

    @Test
    void capturePaymentShouldReturn500OnException() {
        PaymentEntity entity = new PaymentEntity();
        when(paymentService.capturePayment("PMT001")).thenThrow(new PaymentFailedException(entity, "fail"));
        when(paymentService.getResponseEntity(500, entity)).thenReturn(ResponseEntity.status(500).build());

        ResponseEntity<PaymentResponse> result = controller.capturePayment("PMT001");

        assertThat(result.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void voidPaymentShouldReturn202() {
        PaymentResponse response = PaymentResponse.builder().paymentId("PMT001").build();
        when(paymentService.voidPayment("PMT001")).thenReturn(response);

        ResponseEntity<PaymentResponse> result = controller.voidPayment("PMT001");

        assertThat(result.getStatusCode().value()).isEqualTo(202);
    }

    @Test
    void voidPaymentShouldReturn500OnException() {
        PaymentEntity entity = new PaymentEntity();
        when(paymentService.voidPayment("PMT001")).thenThrow(new PaymentFailedException(entity, "fail"));
        when(paymentService.getResponseEntity(500, entity)).thenReturn(ResponseEntity.status(500).build());

        ResponseEntity<PaymentResponse> result = controller.voidPayment("PMT001");

        assertThat(result.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void refundPaymentShouldReturn202() {
        PaymentResponse response = PaymentResponse.builder().paymentId("PMT001").build();
        when(paymentService.refundPayment(eq("PMT001"), any())).thenReturn(response);

        ResponseEntity<PaymentResponse> result = controller.refundPayment("PMT001", new RefundRequest());

        assertThat(result.getStatusCode().value()).isEqualTo(202);
    }

    @Test
    void refundPaymentShouldReturn500OnException() {
        PaymentEntity entity = new PaymentEntity();
        when(paymentService.refundPayment(eq("PMT001"), any())).thenThrow(new PaymentFailedException(entity, "fail"));
        when(paymentService.getResponseEntity(500, entity)).thenReturn(ResponseEntity.status(500).build());

        ResponseEntity<PaymentResponse> result = controller.refundPayment("PMT001", new RefundRequest());

        assertThat(result.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void getPaymentShouldReturn200() {
        PaymentResponse response = PaymentResponse.builder().paymentId("PMT001").build();
        when(paymentService.getPayment("PMT001")).thenReturn(response);

        ResponseEntity<PaymentResponse> result = controller.getPayment("PMT001");

        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getPaymentShouldReturn404OnException() {
        when(paymentService.getPayment("PMT001")).thenThrow(new PaymentFailedException(null, "not found"));
        when(paymentService.getResponseEntity(404, null)).thenReturn(ResponseEntity.status(404).build());

        ResponseEntity<PaymentResponse> result = controller.getPayment("PMT001");

        assertThat(result.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getAllPaymentsShouldReturn200() {
        when(paymentService.getAllPayments(20)).thenReturn(List.of());

        ResponseEntity<List<PaymentResponse>> result = controller.getAllPayments(20);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }
}
