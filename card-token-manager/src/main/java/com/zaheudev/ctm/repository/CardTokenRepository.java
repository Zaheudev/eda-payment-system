package com.zaheudev.ctm.repository;

import com.zaheudev.ctm.entity.CardTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardTokenRepository extends JpaRepository<CardTokenEntity, String> {
     Optional<CardTokenEntity> findByTokenRef(String token);
}
