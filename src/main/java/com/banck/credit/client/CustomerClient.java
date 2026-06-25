package com.banck.credit.client;

import com.banck.credit.client.dto.Customer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerClient {

    private final WebClient webClient;

    public CustomerClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Customer> getCustomerById(String customerId) {

        return webClient.get()
                .uri("http://localhost:8085/customers/" + customerId)
                .retrieve()
                .bodyToMono(Customer.class);
    }
}
