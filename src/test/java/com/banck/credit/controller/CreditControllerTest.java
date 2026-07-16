package com.banck.credit.controller;

import com.banck.credit.dto.PaymentRequest;
import com.banck.credit.model.Credit;
import com.banck.credit.service.CreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class CreditControllerTest {

    private CreditService service;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(CreditService.class);
        CreditController controller =
                new CreditController(service);
        webTestClient = WebTestClient
                .bindToController(controller)
                .build();
    }

    @Test
    void create_shouldReturnCredit() {
        Credit credit = Credit.builder()
                .id("cr1")
                .customerId("c1")
                .build();
        when(service.create(any(Credit.class)))
                .thenReturn(Mono.just(credit));
        webTestClient.post()
                .uri("/credits")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(credit)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo("cr1")
                .jsonPath("$.customerId")
                .isEqualTo("c1");
    }

    @Test
    void findAll_shouldReturnCredits() {
        Credit credit1 = Credit.builder()
                .id("cr1")
                .build();
        Credit credit2 = Credit.builder()
                .id("cr2")
                .build();
        when(service.findAll())
                .thenReturn(
                        Flux.just(credit1, credit2)
                );
        webTestClient.get()
                .uri("/credits")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Credit.class)
                .hasSize(2);
    }

    @Test
    void findById_shouldReturnCredit() {
        Credit credit = Credit.builder()
                .id("cr1")
                .customerId("c1")
                .build();
        when(service.findById("cr1"))
                .thenReturn(Mono.just(credit));
        webTestClient.get()
                .uri("/credits/cr1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo("cr1");
    }

    @Test
    void findByCustomerId_shouldReturnCredits() {
        Credit credit = Credit.builder()
                .id("cr1")
                .customerId("c1")
                .build();
        when(service.findByCustomerId("c1"))
                .thenReturn(
                        Flux.just(credit)
                );
        webTestClient.get()
                .uri("/credits/customer/c1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Credit.class)
                .hasSize(1);
    }

    @Test
    void consume_shouldReturnCredit() {
        Credit credit = Credit.builder()
                .id("cr1")
                .build();
        when(service.consume(
                eq("cr1"),
                eq(BigDecimal.valueOf(100))
        ))
                .thenReturn(Mono.just(credit));
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/credits/cr1/consume")
                        .queryParam("amount", 100)
                        .build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo("cr1");
    }

    @Test
    void payCredit_shouldReturnCredit() {
        Credit credit = Credit.builder()
                .id("cr1")
                .build();
        PaymentRequest request = PaymentRequest.builder()
                .accountId("a1")
                .amount(BigDecimal.valueOf(50))
                .build();
        when(service.payCredit(
                eq("cr1"),
                eq("a1"),
                eq(BigDecimal.valueOf(50))
        ))
                .thenReturn(Mono.just(credit));
        webTestClient.post()
                .uri("/credits/cr1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo("cr1");
    }

    @Test
    void delete_shouldCompleteSuccessfully() {
        when(service.delete("cr1"))
                .thenReturn(Mono.empty());
        webTestClient.delete()
                .uri("/credits/cr1")
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void hasOverdueDebt_shouldReturnTrue() {
        when(service.hasOverdueDebt("c1"))
                .thenReturn(Mono.just(true));
        webTestClient.get()
                .uri("/credits/customers/c1/overdue")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(true);
        verify(service).hasOverdueDebt("c1");
    }

    @Test
    void hasOverdueDebt_shouldReturnFalse() {
        when(service.hasOverdueDebt("c1"))
                .thenReturn(Mono.just(false));
        webTestClient.get()
                .uri("/credits/customers/c1/overdue")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(false);
        verify(service).hasOverdueDebt("c1");
    }

}
