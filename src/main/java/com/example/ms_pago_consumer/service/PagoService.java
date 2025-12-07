package com.example.ms_pago_consumer.service;

import com.example.ms_pago_consumer.dto.PagoFeignResponse;
import com.example.ms_pago_consumer.dto.PagoPendienteEvent;
import com.example.ms_pago_consumer.entity.PagoEntity;

public interface PagoService {

    PagoEntity registrarPagoPendiente(PagoPendienteEvent event);

    PagoFeignResponse llamarAzureFunction(PagoEntity pago);

    PagoEntity actualizarPagoOk(Long idPago);

    PagoEntity actualizarPagoError(Long idPago, String motivo);
}