package com.example.mspagoconsumer.service.impl;

import com.example.mspagoconsumer.dto.PagoFeignResponse;
import com.example.mspagoconsumer.dto.PagoRequestToAzure;
import com.example.mspagoconsumer.dto.PagoPendienteEvent;
import com.example.mspagoconsumer.entity.EstadoPagoEntity;
import com.example.mspagoconsumer.entity.MetodoPagoEntity;
import com.example.mspagoconsumer.entity.OrdenEntity;
import com.example.mspagoconsumer.entity.PagoEntity;
import com.example.mspagoconsumer.exception.OrdenNoEncontradaException;
import com.example.mspagoconsumer.feign.PagoFeign;
import com.example.mspagoconsumer.repository.PagoRepository;
import com.example.mspagoconsumer.service.PagoService;
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

    @Override
    public PagoEntity registrarPagoPendiente(PagoPendienteEvent event) {

        PagoEntity pago = PagoEntity.builder()
                .orden(OrdenEntity.builder().id(event.getIdOrden()).build())
                .metodoPago(MetodoPagoEntity.builder().id(event.getIdMetodoPago()).build())
                .monto(event.getMonto())
                .estadoPago(EstadoPagoEntity.builder().id(1l).build())
                .fechaCreacion(LocalDateTime.now())
                .build();

        PagoEntity saved = pagoRepository.save(pago);

        log.info("[PAGO][DB] Pago registrado como PENDIENTE idPago={}", saved.getOrden());
        return saved;
    }

    @Override
    public PagoEntity buscarPagoConIdOrden(Long idOrden){
        return pagoRepository.findByOrdenId(idOrden).orElseThrow( () -> new OrdenNoEncontradaException("Orden no encontrada"));
    }




    @Override
    public PagoFeignResponse llamarAzureFunction(PagoEntity pago) {

        if (pago.getOrden() == null || pago.getOrden().getId() == null) {
            throw new IllegalStateException(
                    "El pago debe tener idPago antes de llamar Azure Function"
            );
        }

        PagoRequestToAzure request = new PagoRequestToAzure(
                pago.getId(),
                pago.getOrden().getId(),
                pago.getMetodoPago().getId(),
                pago.getMonto()
        );

        try {
            log.info("[PAGO][AZURE] Enviando a Azure Function idPago={}, idOrden={}",
                    pago.getId(), pago.getOrden().getId());

            PagoFeignResponse response = pagoFeign.procesarPago(request);

            log.info("[PAGO][AZURE][OK] Respuesta Azure: {}", response);
            return response;

        } catch (Exception ex) {

            log.error("[PAGO][AZURE][ERROR] Azure Function no disponible. idPago={}, error={}",
                    pago.getId(), ex.getMessage());

            throw new RuntimeException("Azure Function no disponible", ex);
        }
    }
    @Override
    public PagoEntity actualizarPagoOk(Long idPago) {

        PagoEntity pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado id=" + idPago));

        pago.setEstadoPago(EstadoPagoEntity.builder().id(2l).build());
        pago.setFechaActualizacion(LocalDateTime.now());

        pagoRepository.save(pago);

        log.info("[PAGO][DB] Pago OK actualizado idPago={}", idPago);
        return pago;
    }

    @Override
    public PagoEntity actualizarPagoError(Long idPago, String motivo) {

        PagoEntity pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado id=" + idPago));

        pago.setEstadoPago(EstadoPagoEntity.builder().id(3l).build());
        pago.setMotivoError(motivo);
        pago.setFechaActualizacion(LocalDateTime.now());

        pagoRepository.save(pago);

        log.warn("[PAGO][DB] Pago marcado como ERROR idPago={}, motivo={}", idPago, motivo);

        return pago;
    }
}