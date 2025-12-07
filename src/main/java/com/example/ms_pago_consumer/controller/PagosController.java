package com.example.ms_pago_consumer.controller;

import com.example.ms_pago_consumer.dto.PagoPendienteEvent;
import com.example.ms_pago_consumer.entity.PagoEntity;
import com.example.ms_pago_consumer.feign.PagoFeign;
import com.example.ms_pago_consumer.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class PagosController {

    private final PagoService pagoService;

    @PostMapping("/procesar")
    public ResponseEntity<?> procesarPago(@RequestBody PagoPendienteEvent event) {

        // 1. Registrar pago en estado PENDIENTE
        PagoEntity pago = pagoService.registrarPagoPendiente(event);

        try {
            // 2. Llamar Azure Function
            pagoService.llamarAzureFunction(pago);

            return ResponseEntity.ok("Pago enviado a Azure correctamente");

        } catch (Exception ex) {

            // 3. Azure caído → marcar ERROR
            pagoService.actualizarPagoError(pago.getIdPago(), "Azure no disponible");

            return ResponseEntity.status(503)
                    .body("Azure Function no disponible. Pago marcado como ERROR.");
        }
    }
}
