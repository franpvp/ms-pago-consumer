package com.example.mspagoconsumer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "METODO_PAGO")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MetodoPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo_pago")
    private Long id;

    @Column(name = "tipo", nullable = false, unique = true, length = 50)
    private String tipo;

}


