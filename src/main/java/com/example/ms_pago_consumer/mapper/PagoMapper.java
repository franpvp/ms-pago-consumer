package com.example.ms_pago_consumer.mapper;

import com.example.ms_pago_consumer.dto.PagoPendienteEvent;
import com.example.ms_pago_consumer.entity.PagoEntity;

import java.time.LocalDateTime;

public class PagoMapper {

    public static PagoEntity fromPendienteEvent(PagoPendienteEvent event) {

        return PagoEntity.builder()
                .idOrden(event.getIdOrden())
                .idMetodoPago(event.getIdMetodoPago())
                .monto(event.getMonto())
                .estado("PENDIENTE")
                .fechaCreacion(LocalDateTime.now())
                .build();
    }
}