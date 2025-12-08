package com.example.mspagoconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PagoRequestToAzure {
    private Long idPago;
    private Long idOrden;
    private Long idMetodoPago;
    private Integer monto;
}