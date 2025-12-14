package com.example.mspagoconsumer.kafka.consumer;

import com.example.mspagoconsumer.dto.PagoErrorEvent;
import com.example.mspagoconsumer.dto.PagoFeignResponse;
import com.example.mspagoconsumer.dto.PagoOkEvent;

import com.example.mspagoconsumer.dto.PagoPendienteEvent;
import com.example.mspagoconsumer.entity.PagoEntity;
import com.example.mspagoconsumer.kafka.producer.PagoResultProducer;
import com.example.mspagoconsumer.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
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
    public void onPagoPendiente(@Payload PagoPendienteEvent event, Acknowledgment ack) {
        log.info("[PAGO-PENDIENTE][IN] idOrden={}, monto={}", event.getIdOrden(), event.getMonto());

        try {
            PagoEntity pago;
            if(event.isReprocesado()){
                pago = pagoService.buscarPagoConIdOrden(event.getIdOrden());
            }else {
                pago = pagoService.registrarPagoPendiente(event);
            }
            PagoFeignResponse response;
            try {
                response = pagoService.llamarAzureFunction(pago);
            } catch (Exception ex) {
                log.error("[AZURE][ERROR] No se pudo contactar Azure Function: {}", ex.getMessage());

                pagoService.actualizarPagoError(pago.getId(), "Azure Function no disponible");

                pagoResultProducer.enviarError(
                        PagoErrorEvent.builder()
                                .idOrden(pago.getOrden().getId())
                                .idPago(pago.getId())
                                .motivoError("ERROR CONEXION")
                                .fechaError(LocalDateTime.now())
                                .build()
                );

                ack.acknowledge();
                return;
            }

            if (response.getStatus().equalsIgnoreCase("OK")) {

                pagoService.actualizarPagoOk(pago.getId());

                pagoResultProducer.enviarOk(
                        PagoOkEvent.builder()
                                .idOrden(pago.getOrden().getId())
                                .idPago(pago.getId())
                                .monto(pago.getMonto())
                                .fechaOk(LocalDateTime.now())
                                .build()
                );

            } else {

                pagoService.actualizarPagoError(pago.getId(), "Azure retornó error");

                pagoResultProducer.enviarError(
                        PagoErrorEvent.builder()
                                .idOrden(pago.getOrden().getId())
                                .idPago(pago.getId())
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