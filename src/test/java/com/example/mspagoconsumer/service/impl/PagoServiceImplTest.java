package com.example.mspagoconsumer.service.impl;


import com.example.mspagoconsumer.dto.PagoFeignResponse;
import com.example.mspagoconsumer.dto.PagoPendienteEvent;
import com.example.mspagoconsumer.dto.PagoRequestToAzure;
import com.example.mspagoconsumer.entity.EstadoPagoEntity;
import com.example.mspagoconsumer.entity.MetodoPagoEntity;
import com.example.mspagoconsumer.entity.OrdenEntity;
import com.example.mspagoconsumer.entity.PagoEntity;
import com.example.mspagoconsumer.exception.OrdenNoEncontradaException;
import com.example.mspagoconsumer.feign.PagoFeign;
import com.example.mspagoconsumer.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private PagoFeign pagoFeign;

    @InjectMocks
    private PagoServiceImpl pagoServiceImpl;

    private PagoPendienteEvent eventoPendienteBase;
    private PagoEntity pagoBase;

    @BeforeEach
    void setUp() {
        eventoPendienteBase = PagoPendienteEvent.builder()
                .idOrden(10L)
                .idMetodoPago(1L)
                .monto(200_000)
                .reprocesado(false)
                .build();

        pagoBase = PagoEntity.builder()
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
    void registrarPagoPendienteTest() {
        // Arrange
        when(pagoRepository.save(any(PagoEntity.class)))
                .thenAnswer(invocation -> {
                    PagoEntity p = invocation.getArgument(0);
                    p.setId(100L);
                    return p;
                });

        // Act
        PagoEntity result = pagoServiceImpl.registrarPagoPendiente(eventoPendienteBase);

        // Assert
        ArgumentCaptor<PagoEntity> captor = ArgumentCaptor.forClass(PagoEntity.class);
        verify(pagoRepository, times(1)).save(captor.capture());

        PagoEntity guardado = captor.getValue();
        assertThat(guardado.getOrden()).isNotNull();
        assertThat(guardado.getOrden().getId()).isEqualTo(eventoPendienteBase.getIdOrden());
        assertThat(guardado.getMetodoPago()).isNotNull();
        assertThat(guardado.getMetodoPago().getId()).isEqualTo(eventoPendienteBase.getIdMetodoPago());
        assertThat(guardado.getMonto()).isEqualTo(eventoPendienteBase.getMonto());
        // estado PENDIENTE (id=1) según convención típica
        assertThat(guardado.getEstadoPago()).isNotNull();
        assertThat(guardado.getEstadoPago().getId()).isEqualTo(1L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void registrarPagoPendienteExceptionTest() {
        // Arrange
        when(pagoRepository.save(any(PagoEntity.class)))
                .thenThrow(new RuntimeException("Error al guardar pago"));

        // Act + Assert
        assertThrows(RuntimeException.class,
                () -> pagoServiceImpl.registrarPagoPendiente(eventoPendienteBase));

        verify(pagoRepository, times(1)).save(any(PagoEntity.class));
    }

    @Test
    void buscarPagoConIdOrdenTest() {
        // Arrange
        Long idOrden = 10L;
        when(pagoRepository.findByOrdenId(idOrden))
                .thenReturn(Optional.of(pagoBase));

        // Act
        PagoEntity result = pagoServiceImpl.buscarPagoConIdOrden(idOrden);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(pagoBase.getId());
        assertThat(result.getOrden().getId()).isEqualTo(idOrden);
        verify(pagoRepository, times(1)).findByOrdenId(idOrden);
    }

    @Test
    void buscarPagoConIdOrdenNoEncontradaTest() {
        // Arrange
        Long idOrden = 999L;
        when(pagoRepository.findByOrdenId(idOrden))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(OrdenNoEncontradaException.class,
                () -> pagoServiceImpl.buscarPagoConIdOrden(idOrden));

        verify(pagoRepository, times(1)).findByOrdenId(idOrden);
    }

    @Test
    void llamarAzureFunctionTest() {
        // Arrange
        PagoEntity pago = pagoBase;

        PagoFeignResponse responseFeign = new PagoFeignResponse(100L, "OK");

        when(pagoFeign.procesarPago(any(PagoRequestToAzure.class)))
                .thenReturn(responseFeign);

        // Act
        PagoFeignResponse result = pagoServiceImpl.llamarAzureFunction(pago);

        // Assert
        ArgumentCaptor<PagoRequestToAzure> captor = ArgumentCaptor.forClass(PagoRequestToAzure.class);
        verify(pagoFeign, times(1)).procesarPago(captor.capture());

        PagoRequestToAzure requestEnviado = captor.getValue();
        assertThat(requestEnviado.getIdPago()).isEqualTo(pago.getId());
        assertThat(requestEnviado.getIdOrden()).isEqualTo(pago.getOrden().getId());
        assertThat(requestEnviado.getIdMetodoPago()).isEqualTo(pago.getMetodoPago().getId());
        assertThat(requestEnviado.getMonto()).isEqualTo(pago.getMonto());

        assertThat(result).isSameAs(responseFeign);
    }

    @Test
    void llamarAzureFunctionExceptionTest() {
        // Arrange
        PagoEntity pago = pagoBase;

        when(pagoFeign.procesarPago(any(PagoRequestToAzure.class)))
                .thenThrow(new RuntimeException("Error Azure"));

        // Act + Assert
        assertThrows(RuntimeException.class,
                () -> pagoServiceImpl.llamarAzureFunction(pago));

        verify(pagoFeign, times(1)).procesarPago(any(PagoRequestToAzure.class));
    }

    @Test
    void actualizarPagoOkTest() {
        // Arrange
        Long idPago = 100L;
        PagoEntity pagoPendiente = PagoEntity.builder()
                .id(idPago)
                .orden(OrdenEntity.builder().id(10L).build())
                .metodoPago(MetodoPagoEntity.builder().id(1L).build())
                .monto(200_000)
                .estadoPago(EstadoPagoEntity.builder().id(1L).estado("PENDIENTE").build())
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(pagoRepository.findById(idPago))
                .thenReturn(Optional.of(pagoPendiente));

        when(pagoRepository.save(any(PagoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PagoEntity result = pagoServiceImpl.actualizarPagoOk(idPago);

        // Assert
        ArgumentCaptor<PagoEntity> captor = ArgumentCaptor.forClass(PagoEntity.class);
        verify(pagoRepository, times(1)).save(captor.capture());

        PagoEntity actualizado = captor.getValue();
        assertThat(actualizado.getId()).isEqualTo(idPago);
        assertThat(actualizado.getEstadoPago()).isNotNull();
        assertThat(actualizado.getEstadoPago().getId()).isEqualTo(2L);

        assertThat(result.getEstadoPago().getId()).isEqualTo(2L);
    }

    @Test
    void actualizarPagoOkNoEncontradoTest() {
        // Arrange
        Long idPago = 999L;
        when(pagoRepository.findById(idPago))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RuntimeException.class,
                () -> pagoServiceImpl.actualizarPagoOk(idPago));

        verify(pagoRepository, times(1)).findById(idPago);
        verify(pagoRepository, never()).save(any(PagoEntity.class));
    }

    @Test
    void actualizarPagoErrorTest() {
        // Arrange
        Long idPago = 100L;
        String motivo = "Azure retornó ERROR";

        PagoEntity pagoPendiente = PagoEntity.builder()
                .id(idPago)
                .orden(OrdenEntity.builder().id(10L).build())
                .metodoPago(MetodoPagoEntity.builder().id(1L).build())
                .monto(200_000)
                .estadoPago(EstadoPagoEntity.builder().id(1L).estado("PENDIENTE").build())
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now().minusMinutes(5))
                .build();

        when(pagoRepository.findById(idPago))
                .thenReturn(Optional.of(pagoPendiente));

        when(pagoRepository.save(any(PagoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PagoEntity result = pagoServiceImpl.actualizarPagoError(idPago, motivo);

        // Assert
        ArgumentCaptor<PagoEntity> captor = ArgumentCaptor.forClass(PagoEntity.class);
        verify(pagoRepository, times(1)).save(captor.capture());

        PagoEntity actualizado = captor.getValue();
        assertThat(actualizado.getEstadoPago()).isNotNull();
        assertThat(actualizado.getEstadoPago().getId()).isEqualTo(3L);
        assertThat(actualizado.getMotivoError()).isEqualTo(motivo);
        assertThat(actualizado.getFechaActualizacion()).isNotNull();

        assertThat(result.getEstadoPago().getId()).isEqualTo(3L);
        assertThat(result.getMotivoError()).isEqualTo(motivo);
    }

    @Test
    void actualizarPagoErrorNoEncontradoTest() {
        // Arrange
        Long idPago = 999L;
        when(pagoRepository.findById(idPago))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RuntimeException.class,
                () -> pagoServiceImpl.actualizarPagoError(idPago, "ERROR"));

        verify(pagoRepository, times(1)).findById(idPago);
        verify(pagoRepository, never()).save(any(PagoEntity.class));
    }
}
