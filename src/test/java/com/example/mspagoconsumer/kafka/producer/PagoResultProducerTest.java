package com.example.mspagoconsumer.kafka.producer;

import com.example.mspagoconsumer.dto.PagoErrorEvent;
import com.example.mspagoconsumer.dto.PagoOkEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PagoResultProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PagoResultProducer pagoResultProducer;

    @BeforeEach
    void setUp() {
        // Inyectamos los @Value a mano para el escenario de test
        ReflectionTestUtils.setField(pagoResultProducer, "pagoOkTopic", "pago-ok-test");
        ReflectionTestUtils.setField(pagoResultProducer, "pagoErrorTopic", "pago-error-test");
    }

    @Test
    void enviarOkTest() {
        // Arrange
        PagoOkEvent event = PagoOkEvent.builder()
                .idOrden(10L)
                .idPago(100L)
                .monto(200_000)
                .fechaOk(LocalDateTime.now())
                .build();

        // Act
        pagoResultProducer.enviarOk(event);

        // Assert
        String expectedKey = event.getIdOrden().toString();
        verify(kafkaTemplate, times(1))
                .send(eq("pago-ok-test"), eq(expectedKey), eq(event));
    }

    @Test
    void enviarErrorTest() {
        // Arrange
        PagoErrorEvent event = PagoErrorEvent.builder()
                .idOrden(20L)
                .idPago(200L)
                .motivoError("Azure retornó ERROR")
                .fechaError(LocalDateTime.now())
                .monto(200_000)
                .build();

        // Act
        pagoResultProducer.enviarError(event);

        // Assert
        String expectedKey = event.getIdOrden().toString();
        verify(kafkaTemplate, times(1))
                .send(eq("pago-error-test"), eq(expectedKey), eq(event));
    }
}
