package com.example.mspagoconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class PagoPendienteEvent {
    private Long idOrden;
    private Long idMetodoPago;
    private Integer monto;
    private boolean reprocesado;
}
