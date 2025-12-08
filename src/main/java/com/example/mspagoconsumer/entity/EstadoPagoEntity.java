package com.example.mspagoconsumer.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ESTADO_PAGO")
@Getter
@Setter
@Builder
public class EstadoPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_pago")
    private Long id;

    @Column(name = "estado", nullable = false, unique = true, length = 50)
    private String estado;

}

