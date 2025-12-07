package com.example.ms_pago_consumer.feign;

import com.example.ms_pago_consumer.dto.PagoFeignResponse;
import com.example.ms_pago_consumer.dto.PagoRequestToAzure;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "pagoFeign",
        url = "${azure.functions.pagos}"
)
public interface PagoFeign {

    @PostMapping("/api/pagos")
    PagoFeignResponse procesarPago(PagoRequestToAzure request);
}