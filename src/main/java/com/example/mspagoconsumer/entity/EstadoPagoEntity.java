package com.example.mspagoconsumer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ESTADO_PAGO")
@NoArgsConstructor
@AllArgsConstructor
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

