package com.example.mspagoconsumer.service;

import com.example.mspagoconsumer.dto.PagoFeignResponse;
import com.example.mspagoconsumer.dto.PagoPendienteEvent;
import com.example.mspagoconsumer.entity.PagoEntity;

public interface PagoService {

    PagoEntity registrarPagoPendiente(PagoPendienteEvent event);

    PagoEntity buscarPagoConIdOrden(Long idOrden);

    PagoFeignResponse llamarAzureFunction(PagoEntity pago);

    PagoEntity actualizarPagoOk(Long idPago);

    PagoEntity actualizarPagoError(Long idPago, String motivo);
}