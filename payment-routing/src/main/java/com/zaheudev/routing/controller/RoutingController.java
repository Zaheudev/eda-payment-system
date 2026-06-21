package com.zaheudev.routing.controller;

import com.zaheudev.routing.service.RoutingService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
public class RoutingController {
    @Autowired
    RoutingService routingService;

    @GetMapping("/api/v1/routing-costs")
    public ResponseEntity<BigDecimal[]> getTotalCosts(){
        try{
            return ResponseEntity.ok(routingService.getTotalCosts());
        }catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }
}
