package com.example.mspagoconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class PagoErrorEvent {
    private Long idOrden;
    private Long idPago;
    private String motivoError;
    private LocalDateTime fechaError;
    private Integer monto;
}