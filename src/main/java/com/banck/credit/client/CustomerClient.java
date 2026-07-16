package com.banck.credit.client;

import com.banck.credit.client.dto.Customer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerClient {
    private final WebClient webClient;

    public CustomerClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @CircuitBreaker(name = "customerService", fallbackMethod = "getCustomerFallback")
    public Mono<Customer> getCustomerById(String customerId) {
        return webClient.get()
                .uri("http://customer-service/customers/{id}", customerId)
                .retrieve()
                .bodyToMono(Customer.class);
    }

    public Mono<Customer> getCustomerFallback(String customerId, Throwable ex) {
        return Mono.error(
                new RuntimeException(
                        "Customer Service unavailable", ex));
    }
}
