package com.example.ms_pago_consumer.repository;

import com.example.ms_pago_consumer.entity.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoRepository extends JpaRepository<PagoEntity, Long> { }
