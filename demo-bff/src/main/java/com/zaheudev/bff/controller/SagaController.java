package com.zaheudev.bff.controller;

import com.zaheudev.bff.model.SagaState;
import com.zaheudev.bff.service.SagaProjectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@CrossOrigin(origins = "*")
public class SagaController {

    private final SagaProjectionService saga;

    public SagaController(SagaProjectionService saga) {
        this.saga = saga;
    }

    @GetMapping("/api/sagas")
    public Collection<SagaState> getAllSagas() {
        return saga.getAllSagas();
    }

    @GetMapping("/api/sagas/{paymentId}")
    public ResponseEntity<SagaState> getSaga(@PathVariable String paymentId) {
        SagaState state = saga.getSaga(paymentId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }
}
