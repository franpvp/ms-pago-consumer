package com.example.mspagoconsumer.kafka.consumer;


import com.example.mspagoconsumer.dto.PagoErrorEvent;
import com.example.mspagoconsumer.dto.PagoFeignResponse;
import com.example.mspagoconsumer.dto.PagoOkEvent;
import com.example.mspagoconsumer.dto.PagoPendienteEvent;
import com.example.mspagoconsumer.entity.EstadoPagoEntity;
import com.example.mspagoconsumer.entity.MetodoPagoEntity;
import com.example.mspagoconsumer.entity.OrdenEntity;
import com.example.mspagoconsumer.entity.PagoEntity;
import com.example.mspagoconsumer.kafka.producer.PagoResultProducer;
import com.example.mspagoconsumer.service.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoConsumerTest {

    @Mock
    private PagoService pagoService;

    @Mock
    private PagoResultProducer pagoResultProducer;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private PagoConsumer pagoConsumer;

    private PagoPendienteEvent eventoPendiente;
    private PagoEntity pagoPendiente;

    @BeforeEach
    void setUp() {
        eventoPendiente = PagoPendienteEvent.builder()
                .idOrden(10L)
                .idMetodoPago(1L)
                .monto(200_000)
                .reprocesado(false)
                .build();

        pagoPendiente = PagoEntity.builder()
                .id(100L)
                .orden(OrdenEntity.builder().id(10L).build())
                .metodoPago(MetodoPagoEntity.builder().id(1L).build())
                .monto(200_000)
                .estadoPago(EstadoPagoEntity.builder().id(1L).estado("PENDIENTE").build())
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    @Test
    void escucharPagoPendienteOkTest() {
        // Arrange
        // 1) Registrar el pago pendiente
        when(pagoService.registrarPagoPendiente(eventoPendiente))
                .thenReturn(pagoPendiente);

        // 2) Azure responde OK
        PagoFeignResponse responseFeign = new PagoFeignResponse(100L, "OK");
        when(pagoService.llamarAzureFunction(pagoPendiente))
                .thenReturn(responseFeign);

        // 3) Actualizar pago a OK
        PagoEntity pagoOk = PagoEntity.builder()
                .id(100L)
                .orden(pagoPendiente.getOrden())
                .metodoPago(pagoPendiente.getMetodoPago())
                .monto(pagoPendiente.getMonto())
                .estadoPago(EstadoPagoEntity.builder().id(2L).estado("OK").build())
                .fechaCreacion(pagoPendiente.getFechaCreacion())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(pagoService.actualizarPagoOk(100L))
                .thenReturn(pagoOk);

        // Act
        // Ajusta el nombre del método si en tu clase se llama distinto
        pagoConsumer.onPagoPendiente(eventoPendiente, acknowledgment);

        // Assert
        verify(pagoService, times(1)).registrarPagoPendiente(eventoPendiente);
        verify(pagoService, times(1)).llamarAzureFunction(pagoPendiente);
        verify(pagoService, times(1)).actualizarPagoOk(100L);

        ArgumentCaptor<PagoOkEvent> okEventCaptor = ArgumentCaptor.forClass(PagoOkEvent.class);
        verify(pagoResultProducer, times(1)).enviarOk(okEventCaptor.capture());

        PagoOkEvent okEvent = okEventCaptor.getValue();
        assertThat(okEvent.getIdPago()).isEqualTo(100L);
        assertThat(okEvent.getIdOrden()).isEqualTo(10L);
        assertThat(okEvent.getMonto()).isEqualTo(200_000);

        verify(acknowledgment, times(1)).acknowledge();
        verify(pagoResultProducer, never()).enviarError(any(PagoErrorEvent.class));
    }

    @Test
    void escucharPagoPendienteErrorAzureTest() {
        // Arrange
        when(pagoService.registrarPagoPendiente(eventoPendiente))
                .thenReturn(pagoPendiente);

        // Azure responde ERROR (ajusta si tu PagoFeignResponse tiene otra estructura/campo)
        PagoFeignResponse responseFeign = new PagoFeignResponse(100L, "ERROR");
        when(pagoService.llamarAzureFunction(pagoPendiente))
                .thenReturn(responseFeign);

        PagoEntity pagoError = PagoEntity.builder()
                .id(100L)
                .orden(pagoPendiente.getOrden())
                .metodoPago(pagoPendiente.getMetodoPago())
                .monto(pagoPendiente.getMonto())
                .estadoPago(EstadoPagoEntity.builder().id(3L).estado("ERROR").build())
                .motivoError("Azure retornó ERROR")
                .fechaCreacion(pagoPendiente.getFechaCreacion())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(pagoService.actualizarPagoError(eq(100L), anyString()))
                .thenReturn(pagoError);

        // Act
        pagoConsumer.onPagoPendiente(eventoPendiente, acknowledgment);

        // Assert
        verify(pagoService, times(1)).registrarPagoPendiente(eventoPendiente);
        verify(pagoService, times(1)).llamarAzureFunction(pagoPendiente);
        verify(pagoService, times(1))
                .actualizarPagoError(eq(100L), anyString());

        ArgumentCaptor<PagoErrorEvent> errorEventCaptor = ArgumentCaptor.forClass(PagoErrorEvent.class);
        verify(pagoResultProducer, times(1)).enviarError(errorEventCaptor.capture());

        PagoErrorEvent errorEvent = errorEventCaptor.getValue();
        assertThat(errorEvent.getIdPago()).isEqualTo(100L);
        assertThat(errorEvent.getIdOrden()).isEqualTo(10L);

        verify(pagoResultProducer, never()).enviarOk(any(PagoOkEvent.class));
        verify(acknowledgment, times(1)).acknowledge();
    }
}
