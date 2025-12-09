package com.example.mspagoconsumer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ORDEN")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrdenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private ClienteEntity cliente;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleOrdenEntity> detalles = new ArrayList<>();

    @Column(name = "fecha_orden", nullable = false)
    private LocalDateTime fechaOrden = LocalDateTime.now();

    @Column(name = "estado_orden", nullable = false, length = 50)
    private String estadoOrden;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "id_despacho", referencedColumnName = "id_despacho")
    private DespachoEntity despacho;

    @OneToOne(mappedBy = "orden", fetch = FetchType.LAZY)
    private PagoEntity pago;
}
