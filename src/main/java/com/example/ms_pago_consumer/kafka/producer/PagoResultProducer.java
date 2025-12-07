package com.example.ms_pago_consumer.kafka.producer;

import com.example.ms_pago_consumer.dto.PagoErrorEvent;
import com.example.ms_pago_consumer.dto.PagoOkEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagoResultProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.pago-ok}")
    private String pagoOkTopic;

    @Value("${app.kafka.topic.pago-error}")
    private String pagoErrorTopic;

    public void enviarOk(PagoOkEvent event) {
        String key = event.getIdOrden().toString();
        log.info("[PAGO-OK][OUT] topic={}, key={}, event={}", pagoOkTopic, key, event);
        kafkaTemplate.send(pagoOkTopic, key, event);
    }

    public void enviarError(PagoErrorEvent event) {
        String key = event.getIdOrden().toString();
        log.info("[PAGO-ERROR][OUT] topic={}, key={}, event={}", pagoErrorTopic, key, event);
        kafkaTemplate.send(pagoErrorTopic, key, event);
    }
}