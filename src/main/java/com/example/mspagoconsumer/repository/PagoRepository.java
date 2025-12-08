package com.example.mspagoconsumer.repository;

import com.example.mspagoconsumer.entity.PagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<PagoEntity, Long> {

    Optional<PagoEntity> findByOrdenId(Long idOrden);



}
