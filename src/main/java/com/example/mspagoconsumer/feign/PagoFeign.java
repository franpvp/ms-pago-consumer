package com.example.mspagoconsumer.feign;

import com.example.mspagoconsumer.dto.PagoFeignResponse;
import com.example.mspagoconsumer.dto.PagoRequestToAzure;
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