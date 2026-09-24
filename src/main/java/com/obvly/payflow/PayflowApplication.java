package com.obvly.payflow;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@OpenAPIDefinition(
        info = @Info(
                title = "PayFlow API",
                version = "v1",
                description = "Payment processing platform API"
        )
)

@SpringBootApplication
@EnableCaching
public class PayflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayflowApplication.class, args);
    }
}
