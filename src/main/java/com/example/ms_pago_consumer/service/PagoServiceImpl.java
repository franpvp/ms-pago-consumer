package com.example.ms_pago_consumer.service;

import com.example.ms_pago_consumer.dto.PagoFeignResponse;
import com.example.ms_pago_consumer.dto.PagoRequestToAzure;
import com.example.ms_pago_consumer.dto.PagoPendienteEvent;
import com.example.ms_pago_consumer.entity.PagoEntity;
import com.example.ms_pago_consumer.feign.PagoFeign;
import com.example.ms_pago_consumer.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PagoFeign pagoFeign;

    // ===========================================================
    // 1. Registrar pago pendiente al recibir mensaje Kafka
    // ===========================================================
    @Override
    public PagoEntity registrarPagoPendiente(PagoPendienteEvent event) {

        PagoEntity pago = PagoEntity.builder()
                .idOrden(event.getIdOrden())
                .idMetodoPago(event.getIdMetodoPago())
                .monto(event.getMonto())
                .estado("PENDIENTE")
                .fechaCreacion(LocalDateTime.now())
                .build();

        PagoEntity saved = pagoRepository.save(pago);

        log.info("[PAGO][DB] Pago registrado como PENDIENTE idPago={}", saved.getIdPago());
        return saved;
    }

    // ===========================================================
    // 2. Llamar Azure Function
    // ===========================================================
    @Override
    public PagoFeignResponse llamarAzureFunction(PagoEntity pago) {

        if (pago.getIdPago() == null) {
            throw new IllegalStateException(
                    "El pago debe tener idPago antes de llamar Azure Function"
            );
        }

        PagoRequestToAzure request = new PagoRequestToAzure(
                pago.getIdPago(),
                pago.getIdOrden(),
                pago.getIdMetodoPago(),
                pago.getMonto()
        );

        try {
            log.info("[PAGO][AZURE] Enviando a Azure Function idPago={}, idOrden={}",
                    pago.getIdPago(), pago.getIdOrden());

            // ---- FEIGN CALL ----
            PagoFeignResponse response = pagoFeign.procesarPago(request);

            log.info("[PAGO][AZURE][OK] Respuesta Azure: {}", response);
            return response;

        } catch (Exception ex) {

            log.error("[PAGO][AZURE][ERROR] Azure Function no disponible. idPago={}, error={}",
                    pago.getIdPago(), ex.getMessage());

            // ⚠️ Este error NO se propaga; se controlará arriba en el consumer
            throw new RuntimeException("Azure Function no disponible", ex);
        }
    }

    // ===========================================================
    // 3. Actualizar estado OK
    // ===========================================================
    @Override
    public PagoEntity actualizarPagoOk(Long idPago) {

        PagoEntity pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado id=" + idPago));

        pago.setEstado("OK");
        pago.setFechaActualizacion(LocalDateTime.now());

        pagoRepository.save(pago);

        log.info("[PAGO][DB] Pago OK actualizado idPago={}", idPago);
        return pago;
    }

    // ===========================================================
    // 4. Actualizar estado ERROR
    // ===========================================================
    @Override
    public PagoEntity actualizarPagoError(Long idPago, String motivo) {

        PagoEntity pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado id=" + idPago));

        pago.setEstado("ERROR");
        pago.setMotivoError(motivo);
        pago.setFechaActualizacion(LocalDateTime.now());

        pagoRepository.save(pago);

        log.warn("[PAGO][DB] Pago marcado como ERROR idPago={}, motivo={}", idPago, motivo);

        return pago;
    }
}