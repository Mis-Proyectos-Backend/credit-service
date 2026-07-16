package com.banck.credit.client;

import com.banck.credit.client.dto.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerClientTest {

    @Mock
    private WebClient.Builder builder;

    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private CustomerClient customerClient;

    @BeforeEach
    void setUp() {

        when(builder.build()).thenReturn(webClient);

        customerClient = new CustomerClient(builder);
    }

    @Test
    void getCustomerById_shouldReturnCustomer() {

        Customer customer = Customer.builder()
                .id("c1")
                .build();

        doReturn(requestHeadersUriSpec)
                .when(webClient)
                .get();

        doReturn(requestHeadersSpec)
                .when(requestHeadersUriSpec)
                .uri(
                        eq("http://customer-service/customers/{id}"),
                        eq("c1")
                );

        when(requestHeadersSpec.retrieve())
                .thenReturn(responseSpec);

        when(responseSpec.bodyToMono(Customer.class))
                .thenReturn(Mono.just(customer));

        StepVerifier.create(customerClient.getCustomerById("c1"))
                .expectNext(customer)
                .verifyComplete();
    }

    @Test
    void getCustomerFallback_shouldReturnError() {

        StepVerifier.create(
                        customerClient.getCustomerFallback(
                                "c1",
                                new RuntimeException("error")))
                .expectErrorMatches(ex ->
                        ex instanceof RuntimeException
                                && ex.getMessage().equals("Customer Service unavailable"))
                .verify();
    }
}