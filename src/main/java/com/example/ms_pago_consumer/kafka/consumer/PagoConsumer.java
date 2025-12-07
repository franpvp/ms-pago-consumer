package com.example.ms_pago_consumer.kafka.consumer;

import com.example.ms_pago_consumer.dto.PagoErrorEvent;
import com.example.ms_pago_consumer.dto.PagoFeignResponse;
import com.example.ms_pago_consumer.dto.PagoOkEvent;
import com.example.ms_pago_consumer.dto.PagoPendienteEvent;
import com.example.ms_pago_consumer.entity.PagoEntity;
import com.example.ms_pago_consumer.kafka.producer.PagoResultProducer;
import com.example.ms_pago_consumer.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagoConsumer {

    private final PagoService pagoService;
    private final PagoResultProducer pagoResultProducer;

    @KafkaListener(
            topics = "${app.kafka.topic.pago-pendiente}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPagoPendiente(
            @Payload PagoPendienteEvent event,
            Acknowledgment ack
    ) {

        log.info("[PAGO-PENDIENTE][IN] idOrden={}, monto={}",
                event.getIdOrden(), event.getMonto());

        try {
            // 1) Guardar pago en BD como PENDIENTE
            PagoEntity pago = pagoService.registrarPagoPendiente(event);

            // 2) Llamar Azure
            PagoFeignResponse response;
            try {
                response = pagoService.llamarAzureFunction(pago);
            } catch (Exception ex) {
                log.error("[AZURE][ERROR] No se pudo contactar Azure Function: {}", ex.getMessage());

                pagoService.actualizarPagoError(pago.getIdPago(), "Azure Function no disponible");

                pagoResultProducer.enviarError(
                        PagoErrorEvent.builder()
                                .idOrden(pago.getIdOrden())
                                .idPago(pago.getIdPago())
                                .motivoError("Azure Function no disponible")
                                .fechaError(LocalDateTime.now())
                                .build()
                );

                ack.acknowledge();
                return;
            }

            // 3) Azure respondió OK o ERROR
            if (response.getStatus().equalsIgnoreCase("OK")) {

                pagoService.actualizarPagoOk(pago.getIdPago());

                pagoResultProducer.enviarOk(
                        PagoOkEvent.builder()
                                .idOrden(pago.getIdOrden())
                                .idPago(pago.getIdPago())
                                .monto(pago.getMonto())
                                .fechaOk(LocalDateTime.now())
                                .build()
                );

            } else {

                pagoService.actualizarPagoError(pago.getIdPago(), "Azure retornó error");

                pagoResultProducer.enviarError(
                        PagoErrorEvent.builder()
                                .idOrden(pago.getIdOrden())
                                .idPago(pago.getIdPago())
                                .motivoError("Azure retornó ERROR")
                                .fechaError(LocalDateTime.now())
                                .build()
                );
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Error no controlado: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }
}